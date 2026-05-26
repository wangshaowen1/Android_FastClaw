package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 确认或否认当前简历请求体（求职 Agent）
 *
 * @param sessionId       候选人求职 Agent 会话 ID（必填）
 * @param conversationId  对话 ID，用于消息和事件归属校验（必填）
 * @param clientActionId  客户端动作 ID，用于确认或否认历史简历幂等（必填）
 * @param resumeId        当前可确认简历 ID（必填）
 * @param confirmed       是否确认该历史简历为本人当前可用简历：true 确认 / false 否认（必填）
 */
data class ResumeConfirmBody(
    val sessionId: String?,
    val conversationId: String?,
    val clientActionId: String?,
    val resumeId: String?,
    val confirmed: Boolean,
)