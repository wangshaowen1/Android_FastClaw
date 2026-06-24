package com.shiyuan.fastclaw.core.network.apis.request

data class MockInterviewStartBody(
    /** 模拟面试入口消息 ID */
    val entryMessageId: String? = null,
    /** 模拟面试入口来源，如 QUICK_ENTRY */
    val entrySource: String? = null,
    /** 发起模拟面试使用的简历 ID */
    val resumeId: String? = null,
    /** 扣费或权益占用请求幂等 ID */
    val chargeRequestId: String? = null,
)

data class MockInterviewStreamBody(
    /** 模拟面试会话 ID */
    val interviewId: String? = null,
    /** 本次流式交互动作类型 */
    val actionType: String? = null,
    /** 客户端动作幂等 ID */
    val clientActionId: String? = null,
    /** 端侧已知最后一条消息序号 */
    val lastMessageSeq: Long? = null,
    /** 动作业务参数 */
    val payload: Map<String, Any?>? = null,
    /** 客户端当前状态快照 */
    val clientSnapshot: Map<String, Any?>? = null,
)

data class MockInterviewAudioTokenBody(
    /** 模拟面试会话 ID */
    val interviewId: String? = null,
    /** 音频 token 所属业务类型 */
    val bizType: String = "MOCK_INTERVIEW",
    /** 需要申请的音频服务类型列表 */
    val serviceTypes: List<String> = listOf("ASR", "TTS"),
)

data class MockInterviewReportProgressBody(
    /** 模拟面试报告 ID */
    val mockReportId: String? = null,
    /** 模拟面试会话 ID */
    val interviewId: String? = null,
)

data class MockInterviewReportRetryBody(
    /** 原模拟面试报告 ID */
    val mockReportId: String? = null,
    /** 模拟面试会话 ID */
    val interviewId: String? = null,
    /** 报告重试请求幂等 ID */
    val reportRetryId: String? = null,
)
