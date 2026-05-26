package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 拉取增量消息请求体（求职 Agent，拉取游标之后的新消息）
 *
 * @param sessionId         候选人求职 Agent 会话 ID（必填）
 * @param conversationId    对话 ID，用于校验增量消息拉取归属。
 * @param afterCursor       增量起始游标，服务端返回该游标之后的新消息
 * @param limit             本次拉取数量上限，默认由服务端兜底限制
 */
data class MessagePullBody(
    val sessionId: String?,
    val conversationId: String?,
    val afterCursor: String? = null,
    val limit: Int? = null,
)