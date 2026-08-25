package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 同意发送简历请求体。
 *
 * @param sessionId 本地会话业务 ID
 */
data class ResumeSendAgreeBody(
    val sessionId: String,
)
