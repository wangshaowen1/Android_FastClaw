package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 更新顶部求职状态请求体
 */
data class JobStatusUpdateBody(
    val sessionId: String?,
    val conversationId: String?,
    val statusCode: String?,
    val clientRequestId: String? = null,
)
