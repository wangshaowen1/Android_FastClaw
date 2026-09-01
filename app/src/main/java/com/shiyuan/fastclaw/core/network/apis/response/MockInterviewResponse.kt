package com.shiyuan.fastclaw.core.network.apis.response

data class MockInterviewStartData(
    /** 模拟面试会话 ID */
    val interviewId: String? = null,
    /** 求职 Agent 对话 ID */
    val conversationId: String? = null,
    /** 触发模拟面试入口的消息 ID */
    val entryMessageId: String? = null,
    /** 扣费状态，如 DEDUCT_SUCCESS / DEDUCT_FAILED */
    val chargeStatus: String? = null,
    /** 扣费失败或扣费异常原因 */
    val chargeReason: String? = null,
    /** 本次扣费数量 */
    val chargeAmount: Int? = null,
    /** 面试会话状态，如 INTERVIEWING */
    val sessionStatus: String? = null,
    /** 目标岗位状态 */
    val targetJobStatus: String? = null,
    /** 服务端提示文案 */
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


data class MockInterviewForceCancelData(
    /** 模拟面试会话 ID */
    val interviewId: String? = null,
    /** 面试会话状态 */
    val sessionStatus: String? = null,
    /** 面试房间状态 */
    val roomState: String? = null,
    /** 强制结束原因 */
    val cancelReason: String? = null,
    /** 服务端提示文案 */
    val message: String? = null,
)

data class MockInterviewReportProgressData(
    /** 模拟面试报告 ID */
    val mockReportId: String? = null,
    /** 模拟面试会话 ID */
    val interviewId: String? = null,
    /** 报告生成状态：GENERATING / SUCCESS / FAILED 等 */
    val reportStatus: String? = null,
    /** 当前生成步骤 */
    val progressStep: String? = null,
    /** 生成进度百分比 */
    val progressPercent: Int? = null,
    /** 生成进度展示文案 */
    val progressText: String? = null,
    /** 是否允许重试生成 */
    val canRetry: Boolean? = null,
    /** 下一次建议轮询间隔秒数 */
    val nextPollSeconds: Int? = null,
    /** 报告详情是否可查看 */
    val detailEnabled: Boolean? = null,
    /** 报告总分 */
    val totalScore: Int? = null,
    /** 分数等级 */
    val scoreLevel: String? = null,
    /** 面试时长，单位秒 */
    val interviewDurationSec: Int? = null,
    /** 失败原因 */
    val failReason: String? = null,
    /** 状态更新时间 */
    val updateTime: String? = null,
)
