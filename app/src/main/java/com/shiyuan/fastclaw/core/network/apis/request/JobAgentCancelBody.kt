package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 取消流式生成请求体（求职 Agent）
 *
 * @param requestId       本次需要取消的流式请求 ID（必填）
 * @param conversationId  对话 ID；可选，用于端侧传入时做前置归属校验
 * @param cancelReason    取消原因：USER_CANCEL 用户主动取消 / NEW_MESSAGE_INTERRUPTED 新消息打断
 */
data class JobAgentCancelBody(
    val requestId: String?,
    val conversationId: String? = null,
    val cancelReason: String? = null,
)