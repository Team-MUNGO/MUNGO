package com.example.poco.data.assistant

import com.example.poco.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import timber.log.Timber

class AssistantRemoteDataSourceImpl @Inject constructor(
    private val anonymous: FirebaseAnonymousAuth
) : AssistantRemoteDataSource {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    private val baseUrl = BuildConfig.ASSISTANT_URL.trimEnd('/')

    // Short-lived session credentials stay in memory; no preference, backup, log or APK secret.
    @Volatile private var session: Session? = null
    override val configured get() = baseUrl.startsWith("https://")

    // Judge/production path: automatic Firebase anonymous auth. Invitation codes remain for dev builds.
    override val anonymousAuth get() = anonymous.available
    override val connected get() = session?.expiresAt?.let { it > System.currentTimeMillis() } ==
        true

    override suspend fun connect(invitation: String) {
        if (invitation.isBlank() && anonymous.available) {
            if (!configured) throw AssistantException("NOT_CONFIGURED", "AI 서버 연결이 아직 설정되지 않았어요.")
            session = anonymous.signIn().let { Session(it.idToken, it.expiresAt) }
            return
        }
        require(Regex("[A-Za-z0-9_-]{43}").matches(invitation)) { "연결 코드를 확인해 주세요." }
        val result = post("session", json.encodeToString(Invitation(invitation)), null)
        val value = json.decodeFromString<Session>(result)
        check(
            Regex("[A-Za-z0-9_-]{43}").matches(value.token) &&
                value.expiresAt > System.currentTimeMillis()
        )
        session = value
    }

    override suspend fun assist(request: AssistRequest): AssistResponse {
        if (!connected && anonymous.available) {
            // ID tokens last one hour; renew silently with the stored anonymous refresh token.
            session = anonymous.signIn().let { Session(it.idToken, it.expiresAt) }
        }
        if (!connected) {
            session = null
            throw AssistantException("UNAUTHENTICATED", "정차 후 AI 연결을 설정해 주세요.")
        }
        val response = post("assist", json.encodeToString(request), session!!.token)
        return try {
            val parsed = json.decodeFromString<AssistResponse>(response)
            runCatching { parsed.validate(request) }.onFailure {
                // 위치 · 질문 내용 없이 어떤 규칙에 걸렸는지 볼 수 있는 값만 남긴다
                Timber.w(
                    "AI 응답 검증 실패: status=%s places=%d source=%s recommended=%s display=%d tts=%d",
                    parsed.status,
                    parsed.places.size,
                    parsed.sources.places,
                    parsed.recommendedPlaceId != null,
                    parsed.displayText.length,
                    parsed.ttsText.length
                )
            }.getOrThrow()
        } catch (e: Exception) {
            Timber.w("AI 응답 해석 실패: %s", e.toString().take(300))
            throw AssistantException("INVALID_RESPONSE", "답변을 확인하지 못했어요. 다시 질문해 주세요.")
        }
    }

    override suspend fun summarizeTrip(request: TripSummaryRequest): String {
        ensureSession()
        val response = post("summary", json.encodeToString(request), session!!.token)
        val parsed = try {
            json.decodeFromString<TripSummaryResponse>(response)
        } catch (_: Exception) {
            throw AssistantException("INVALID_RESPONSE", "응답을 확인하지 못했어요.")
        }
        val text = parsed.text.trim()
        if (parsed.requestId != request.requestId || text.isEmpty() || text.length > 120) {
            throw AssistantException("INVALID_RESPONSE", "응답을 확인하지 못했어요.")
        }
        return text
    }

    private suspend fun ensureSession() {
        if (!connected && anonymous.available) {
            session = anonymous.signIn().let { Session(it.idToken, it.expiresAt) }
        }
        if (!connected) {
            session = null
            throw AssistantException("UNAUTHENTICATED", "정차 후 AI 연결을 설정해 주세요.")
        }
    }

    private suspend fun post(path: String, body: String, token: String?): String {
        if (!configured) throw AssistantException("NOT_CONFIGURED", "AI 서버 연결이 아직 설정되지 않았어요.")
        val request = Request.Builder().url("$baseUrl/$path")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .apply { token?.let { header("Authorization", "Bearer $it") } }.build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.w("AI 서버 네트워크 실패 (%s): %s", path, e.toString())
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            AssistantException(
                                "NETWORK_ERROR",
                                "연결을 확인하고 다시 질문해 주세요."
                            )
                        )
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        try {
                            val source = it.body.source()
                            if (source.request(100001)) error("oversize response")
                            val text = source.readUtf8()
                            if (!it.isSuccessful) {
                                if (it.code == 401) session = null
                                val code = runCatching {
                                    json.decodeFromString<Failure>(text).code
                                }.getOrDefault("SERVICE_UNAVAILABLE")
                                val message = when (code) {
                                    "UNAUTHENTICATED", "INVALID_INVITATION" -> {
                                        if (anonymous.available) {
                                            "AI 인증이 만료되었어요. 다시 질문해 주세요."
                                        } else {
                                            "AI 연결 코드가 만료되었거나 올바르지 않아요."
                                        }
                                    }
                                    "RATE_LIMITED" -> "요청이 많아요. 잠시 후 다시 질문해 주세요."
                                    "MAPS_UNAVAILABLE" -> "장소를 검색하지 못했어요. 잠시 후 다시 시도해 주세요."
                                    "MAPS_LIMITED" -> "오늘 장소 검색 한도에 도달했어요."
                                    "LOCATION_UNAVAILABLE" -> "최근 위치를 확인하지 못했어요."
                                    "CATALOG_UNAVAILABLE" -> "휴게소 정보를 준비하고 있어요."
                                    else -> "답변을 받지 못했어요. 잠시 후 다시 질문해 주세요."
                                }
                                throw AssistantException(code, message)
                            }
                            continuation.resume(text)
                        } catch (e: Exception) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    if (e is AssistantException) {
                                        e
                                    } else {
                                        AssistantException(
                                            "INVALID_RESPONSE",
                                            "응답을 확인하지 못했어요."
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            })
        }
    }
    override fun disconnect() {
        session = null
    }

    @Serializable private data class Invitation(val invitation: String)

    @Serializable private data class Session(val token: String, val expiresAt: Long)

    @Serializable private data class Failure(val code: String)
}
