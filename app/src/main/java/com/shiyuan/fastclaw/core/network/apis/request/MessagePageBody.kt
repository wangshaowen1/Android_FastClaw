package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询历史消息分页请求体（求职 Agent，向上翻页加载更早消息）
 *
 * @param sessionId     候选人求职 Agent 会话 ID（必填）
 * @param beforeCursor  向上翻页游标；为空表示从当前最新消息之前查询
 * @param pageSize      分页大小，默认由服务端兜底限制
 */
data class MessagePageBody(
    val sessionId: String?,
    val beforeCursor: String? = null,
    val pageSize: Int? = null,
)