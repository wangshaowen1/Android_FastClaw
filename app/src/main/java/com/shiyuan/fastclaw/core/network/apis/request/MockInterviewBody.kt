package com.shiyuan.fastclaw.core.network.apis.request

data class MockInterviewStartBody(
    val entryMessageId: String? = null,
    val entrySource: String? = null,
    val resumeId: String? = null,
    val chargeRequestId: String? = null,
)

data class MockInterviewStreamBody(
    val interviewId: String? = null,
    val actionType: String? = null,
    val clientActionId: String? = null,
    val lastMessageSeq: Long? = null,
    val payload: Map<String, Any?>? = null,
    val clientSnapshot: Map<String, Any?>? = null,
)

data class MockInterviewAudioTokenBody(
    val interviewId: String? = null,
    val bizType: String = "MOCK_INTERVIEW",
    val serviceTypes: List<String> = listOf("ASR", "TTS"),
)

data class MockInterviewReportProgressBody(
    val mockReportId: String? = null,
    val interviewId: String? = null,
)

data class MockInterviewReportRetryBody(
    val mockReportId: String? = null,
    val interviewId: String? = null,
    val reportRetryId: String? = null,
)
