package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 进入或恢复会话请求体
 *
 * @param sessionId        候选人求职 Agent 会话 ID；为空表示首次进入或由服务端恢复最新会话
 * @param conversationId   对话 ID；为空表示由服务端恢复或创建当前对话
 * @param clientRequestId  客户端请求 ID，用于进入页面动作幂等
 * @param latestCursor     客户端已持有的最新游标，服务端据此判断是否需要返回增量消息
 * @param messageLimit     首次恢复消息数量，默认由服务端兜底限制
 */
data class SessionEnterBody(
    val sessionId: String? = null,
    val conversationId: String? = null,
    val clientRequestId: String? = null,
    val latestCursor: String? = null,
    val messageLimit: Int? = null,
)