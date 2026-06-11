package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询属性数组候选值请求体（求职 Agent）
 *
 * @param sessionId        候选人求职 Agent 会话 ID（必填）
 * @param conversationId    对话 ID，用于消息和队列归属校验（必填）
 * @param questionQueueId   属性候选值所在的问题队列 ID（必填）
 * @param attributeGroupCode 属性组编码（必填）
 */
data class AttributeArrayQueryBody(
    val sessionId: String?,
    val conversationId: String?,
    val questionQueueId: String?,
    val attributeGroupCode: String?,
)
