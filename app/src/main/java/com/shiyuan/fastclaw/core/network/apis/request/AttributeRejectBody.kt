package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 拒绝属性候选值请求体（求职 Agent）
 *
 * @param sessionId        候选人求职 Agent 会话 ID（必填）
 * @param conversationId   对话 ID，用于消息和队列归属校验（必填）
 * @param clientActionId   客户端动作 ID，用于确认或拒绝动作幂等（必填）
 * @param questionQueueId  属性候选值所在的问题队列 ID（必填）
 * @param reason           拒绝原因或确认备注
 */
data class AttributeRejectBody(
    val sessionId: String?,
    val conversationId: String?,
    val clientActionId: String?,
    val questionQueueId: String?,
    val reason: String? = null,
)