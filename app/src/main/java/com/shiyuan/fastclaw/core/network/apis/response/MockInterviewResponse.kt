package com.shiyuan.fastclaw.core.network.apis.response

data class MockInterviewStartData(
    val interviewId: String? = null,
    val conversationId: String? = null,
    val entryMessageId: String? = null,
    val chargeStatus: String? = null,
    val chargeReason: String? = null,
    val chargeAmount: Int? = null,
    val sessionStatus: String? = null,
    val targetJobStatus: String? = null,
    val message: String? = null,
) {
    fun isDeductSuccess() = chargeStatus == "DEDUCT_SUCCESS"

    fun isDeductFailed() = chargeStatus == "DEDUCT_FAILED"
}

data class MockInterviewUnfinishedData(
    /** 是否存在可恢复的未结束面试 */
    val hasUnfinished: Boolean? = null,
    /** 可恢复的模拟面试会话 ID */
    val interviewId: String? = null,
    /** 会话状态，如 INTERVIEWING */
    val sessionStatus: String? = null,
    /** 房间状态，如 WAITING_USER_ANSWER */
    val roomState: String? = null,
)

data class MockInterviewAudioTokenData(
    val asrToken: String? = null,
    val ttsToken: String? = null,
    val expireAt: String? = null,
    val refreshBeforeSeconds: Int? = null,
)

data class MockInterviewForceCancelData(
    val interviewId: String? = null,
    val sessionStatus: String? = null,
    val roomState: String? = null,
    val cancelReason: String? = null,
    val message: String? = null,
)

data class MockInterviewReportProgressData(
    val mockReportId: String? = null,
    val interviewId: String? = null,
    val reportStatus: String? = null,
    val progressStep: String? = null,
    val progressPercent: Int? = null,
    val progressText: String? = null,
    val canRetry: Boolean? = null,
    val nextPollSeconds: Int? = null,
    val detailEnabled: Boolean? = null,
    val totalScore: Int? = null,
    val scoreLevel: String? = null,
    val interviewDurationSec: Int? = null,
    val failReason: String? = null,
    val updateTime: String? = null,
)
