package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询当前可确认简历请求体（求职 Agent）
 *
 * @param sessionId  候选人求职 Agent 会话 ID；为空时只按当前登录候选人查询可确认简历摘要
 */
data class CurrentResumeQueryBody(
    val sessionId: String? = null,
)