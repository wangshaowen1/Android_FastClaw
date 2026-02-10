package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "storyboardId": "分镜主键id",
 *     "storyBoardDesc": "分镜描述",
 *     "genImagePrompt": "图片生成提示词",
 *     "genVideoPrompt": "视频生成提示词",
 *     "imageUrl": "当前图片url",
 *     "videoUrl": "当前视频url",
 *     "duration": "视频时长(秒)",
 *     "storyId": "剧集 id",
 *     "workId": "作品 ID"
 * }
 */
data class SaveStoryboardBody(
    /**
     * 分镜主键 id
     */
    val storyboardId: String? = null,
    /**
     * 分镜描述
     */
    val storyBoardDesc: String? = null,
    /**
     * 图片生成提示词
     */
    val genImagePrompt: String? = null,
    /**
     * 视频生成提示词
     */
    val genVideoPrompt: String? = null,
    /**
     * 当前图片url
     */
    val imageUrl: String? = null,
    /**
     * 当前视频url
     */
    val videoUrl: String? = null,
    /**
     * 视频时长(秒)
     */
    val duration: Long? = null,
    /**
     * 剧集 id
     */
    val storyId: String? = null,
    /**
     * 作品 ID
     */
    val workId: String? = null
)