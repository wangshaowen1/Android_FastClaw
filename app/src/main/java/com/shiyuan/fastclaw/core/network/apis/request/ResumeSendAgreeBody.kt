package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 处理 HR 索要简历请求体。
 *
 * @param sessionId 本地会话业务 ID
 * @param messageServerId 云信索要简历原消息服务端 ID
 * @param status 候选人处理状态，accepted 为同意，rejected 为拒绝
 */
data class ResumeSendAgreeBody(
    val sessionId: String,
    val messageServerId: String,
    val status: String,
) {
    companion object {
        // 同意发送简历。
        const val STATUS_ACCEPTED = "accepted"

        // 拒绝发送简历。
        const val STATUS_REJECTED = "rejected"
    }
}
