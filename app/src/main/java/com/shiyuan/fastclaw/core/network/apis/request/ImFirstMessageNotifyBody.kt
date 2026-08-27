package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 通知首条聊天消息已发送请求体。
 *
 * @param sessionId 本地会话业务 ID
 */
data class ImFirstMessageNotifyBody(
    val sessionId: String,
)
