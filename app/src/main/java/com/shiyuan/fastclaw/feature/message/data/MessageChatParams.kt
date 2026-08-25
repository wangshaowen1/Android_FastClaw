package com.shiyuan.fastclaw.feature.message.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * IM 单聊详情页启动参数。
 */
@Parcelize
data class MessageChatParams(
    // 聊天会话 id，当前用于草稿和简历请求上下文。
    val sessionId: String,
    // P2P 会话对端云信账号。
    val contactId: String,
    // 会话对象昵称。
    val name: String,
    // 会话对象头像地址。
    val avatar: String?,
    // 当前沟通的岗位ID。
    val currentJobId: Long? = null,
    // 当前沟通的岗位名称。
    val currentJobName: String? = null,
    // 本次是否首次创建。
    val isFirstCreate: Boolean? = false,
    // 本次请求是否切换了会话当前岗位；首次创建或原岗位与请求岗位一致时为 false，不一致并完成更新时为 true。
    val isJobSwitched: Boolean? = true,
) : Parcelable
