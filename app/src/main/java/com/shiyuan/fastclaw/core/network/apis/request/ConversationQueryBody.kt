package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询通信会话请求体（求职 Agent）
 *
 * @param conversationId  通信会话 ID；为空表示查询当前候选人在指定场景下的活跃会话
 * @param sessionId       求职 Agent 状态会话 ID；可选，用于需要同时恢复业务状态时辅助校验
 * @param requestId       流式请求 ID；可选，传入后查询该请求的处理状态，不传默认查询最近一次请求
 * @param sceneType       场景类型：1 / 2
 */
data class ConversationQueryBody(
    val conversationId: String? = null,
    val sessionId: String? = null,
    val requestId: String? = null,
    val sceneType: Int? = null,
)