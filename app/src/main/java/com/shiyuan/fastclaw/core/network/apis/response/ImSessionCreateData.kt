package com.shiyuan.fastclaw.core.network.apis.response

/** 创建企业侧 IM 会话结果中聊天页需要的对端信息。 */
data class ImSessionCreateData(
    val targetBaseAccountId: Long = 0,
    val targetNickname: String? = null,
    val targetAvatarUrl: String? = null,
)
