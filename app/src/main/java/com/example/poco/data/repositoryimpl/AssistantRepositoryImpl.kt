package com.example.poco.data.repositoryimpl

import com.example.poco.data.assistant.AssistRequest
import com.example.poco.data.assistant.AssistantRemoteDataSource
import com.example.poco.data.assistant.TripSummaryRequest
import com.example.poco.data.repository.AssistantRepository
import javax.inject.Inject

class AssistantRepositoryImpl @Inject constructor(private val source: AssistantRemoteDataSource) :
    AssistantRepository {
    override val configured get() = source.configured
    override val connected get() = source.connected
    override val anonymousAuth get() = source.anonymousAuth
    override suspend fun connect(invitation: String) = source.connect(invitation)
    override suspend fun assist(request: AssistRequest) = source.assist(request)
    override suspend fun summarizeTrip(request: TripSummaryRequest) = source.summarizeTrip(request)

    // 개발 단계: 정보 전송 동의를 따로 받지 않고 기본 허용으로 둔다 (주행 중 동의 창 없이 바로 질문 · 휴식 제안).
    // 출시 전에는 처음 한 번 동의를 받아 저장하는 방식으로 되돌릴 것. 'AI 연결 설정 > 연결 해제'는 이번 실행 동안만 끈다
    @Volatile override var consented = true
    override fun disconnect() = source.disconnect()
}
