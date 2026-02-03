package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "storyboardId": "分镜id",
 *     "storyBoardDesc": "分镜描述",
 *     "genImagePrompt": "图片生成提示词",
 *     "genVideoPrompt": "视频生成提示词",
 *     "workId": "作品 ID",
 *     "storyId": "剧集 id"
 * }
 */
data class GenerateStoryboardBody(
    /**
     * 分镜id
     */
    val storyboardId: String?,
    /**
     * 分镜描述
     */
    val storyBoardDesc: String?,
    /**
     * 图片生成提示词
     */
    val genImagePrompt: String?,
    /**
     * 视频生成提示词
     */
    val genVideoPrompt: String?,
    /**
     * 作品 ID
     */
    val workId: String?,
    /**
     * 剧集 id
     */
    val storyId: String?
)