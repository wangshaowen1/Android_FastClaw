package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "storyboardId": "分镜主键id",
 *     "workId": "作品ID",
 *     "storyId": "剧集id",
 *     "sortNo": "序号",
 *     "creationMode": "[枚举]创作模式 [1=图生视频 2=首尾帧视频 3=融合生视频]",
 *     "storyBoardDesc": "分镜描述",
 *     "genImagePrompt": "图片生成提示词",
 *     "genVideoPrompt": "视频生成提示词",
 *     "imageUrl": "当前图片url",
 *     "videoUrl": "当前视频url",
 *     "stepStatus": "[枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]",
 *     "duration": "视频时长(毫秒)",
 *     "taskId": "任务ID (雪花算法)",
 *     "taskType": "[枚举]任务类型 [1=文生文 2=文生图 3=图生图 4=图生视频]"
 * }
 */
@Parcelize
data class SimpleStoryboardInfo(
    /**
     * 分镜主键id
     */
    val storyboardId: String? = null,
    /**
     * 作品ID
     */
    val workId: String? = null,
    /**
     * 剧集id
     */
    val storyId: String? = null,
    /**
     * 序号
     */
    val sortNo: Int? = null,
    /**
     * [枚举]创作模式 [1=图生视频 2=首尾帧视频 3=融合生视频]
     */
    val creationMode: Int? = null,
    /**
     * 分镜描述
     */
    var storyBoardDesc: String? = null,
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
    var imageUrl: String? = null,
    /**
     * 当前视频url
     */
    var videoUrl: String? = null,
    /**
     * [枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    var stepStatus: Int? = null,
    /**
     * 视频时长(毫秒)
     */
    val duration: Long? = null,
    /**
     * 任务ID (雪花算法)
     */
    var taskId: String? = null,
    /**
     * [枚举]任务类型 [1=文生文 2=文生图 3=图生图 4=文生视频 5图生视频 6-视频生视频]
     */
    val taskType: Int? = null,
) : Parcelable {
    val imageGenerating
        get() = when (creationMode) {
            3 -> true
            else -> when (taskType) {
                1, 2, 3 -> stepStatus == 1
                else -> false
            }
        }

    val imageGenerated
        get() = when (creationMode) {
            3 -> true
            else -> when (taskType) {
                1, 2, 3 -> stepStatus == 2
                else -> false
            }
        }

    val failed get() = stepStatus == 3

    val videoGenerating
        get() = when (taskType) {
            4, 5, 6 -> stepStatus == 1
            else -> false
        }
    val videoGenerated
        get() = when (taskType) {
            4, 5, 6 -> stepStatus == 2
            else -> false
        }
}