package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 作品播放事件请求体
 *
 * @param userId 用户 ID
 * @param workId 作品 ID
 * @param storyId 故事 ID
 */
data class PlayEventBody(
    /** 用户 ID */
    val userId: String? = null,
    /** 作品 ID */
    val workId: String? = null,
    /** 故事 ID */
    val storyId: String? = null
) : UserTokenBody()