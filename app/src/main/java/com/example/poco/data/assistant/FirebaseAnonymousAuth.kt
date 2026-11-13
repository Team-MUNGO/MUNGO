package com.example.poco.data.assistant

import android.content.Context
import com.example.poco.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** Short-lived Firebase ID token used as the Bearer credential for the mungo server. */
data class AuthToken(val idToken: String, val expiresAt: Long)

/**
 * Firebase Authentication anonymous sign-in over the public Identity Toolkit REST API.
 * No Google Play services dependency (Pleos/AAOS devices may not ship them).
 * The Web API key is a public project identifier, not a secret; the Gemini key stays server-side.
 * Only the anonymous refresh token is persisted, in no-backup app storage, so one install keeps
 * one uid and per-user server limits cannot be reset by restarting the app.
 */
@Singleton
class FirebaseAnonymousAuth @Inject constructor(@ApplicationContext context: Context) {
    private val apiKey = BuildConfig.FIREBASE_API_KEY
    val available get() = apiKey.isNotEmpty()
    private val store = File(context.noBackupFilesDir, "poco_anonymous_auth")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()

    suspend fun signIn(): AuthToken = mutex.withLock {
        if (!available) throw AssistantException("NOT_CONFIGURED", "AI 서버 연결이 아직 설정되지 않았어요.")
        val saved =
            withContext(Dispatchers.IO) { runCatching { store.readText().trim() }.getOrNull() }
        if (!saved.isNullOrEmpty()) {
            val refreshed = runCatching {
                val body = FormBody.Builder().add("grant_type", "refresh_token")
                    .add("refresh_token", saved).build()
                json.decodeFromString<Refresh>(
                    call("https://securetoken.googleapis.com/v1/token?key=$apiKey", body)
                )
            }
            refreshed.getOrNull()?.let {
                return@withLock persist(it.idToken, it.refreshToken, it.expiresIn)
            }
            if (refreshed.exceptionOrNull() is NetworkFailure) throw networkError()
            // Revoked/invalid refresh token: fall through to a new anonymous account.
        }
        val created = try {
            json.decodeFromString<SignUp>(
                call(
                    "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey",
                    """{"returnSecureToken":true}"""
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                )
            )
        } catch (_: NetworkFailure) {
            throw networkError()
        } catch (_: Exception) {
            throw AssistantException("AUTH_UNAVAILABLE", "AI 인증을 준비하지 못했어요. 잠시 후 다시 시도해 주세요.")
        }
        persist(created.idToken, created.refreshToken, created.expiresIn)
    }

    private suspend fun persist(
        idToken: String,
        refreshToken: String,
        expiresIn: String
    ): AuthToken {
        val seconds = expiresIn.toLongOrNull() ?: 0
        check(idToken.count { it == '.' } == 2 && refreshToken.isNotEmpty() && seconds > 120)
        withContext(Dispatchers.IO) { store.writeText(refreshToken) }
        // Refresh one minute before the server would reject the token.
        return AuthToken(idToken, System.currentTimeMillis() + (seconds - 60) * 1000)
    }

    private fun networkError() = AssistantException("NETWORK_ERROR", "연결을 확인하고 다시 질문해 주세요.")

    private class NetworkFailure : IOException()

    private suspend fun call(url: String, body: RequestBody): String =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(Request.Builder().url(url).post(body).build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(NetworkFailure())
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        val text = runCatching { it.body.string() }.getOrDefault("")
                        // Never surface or log the response body: it contains credentials.
                        if (it.isSuccessful && text.length < 20_000) {
                            continuation.resume(text)
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("auth ${it.code}")
                            )
                        }
                    }
                }
            })
        }

    @Serializable
    private data class SignUp(val idToken: String, val refreshToken: String, val expiresIn: String)

    @Serializable private data class Refresh(
        @SerialName("id_token") val idToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: String
    )
}
