package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "workId": "作品ID",
 *     "storyId": "剧集ID",
 *     "videoUrl": "合成后的视频 URL",
 *     "duration": "视频时长（毫秒），可选"
 * }
 */
data class VideoSynthesisBody(
    val workId: String?,
    val storyId: String?,
    val videoUrl: String?,
    val duration: Long?
) : UserTokenBody()
