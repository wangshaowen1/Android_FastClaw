package com.kgmedia.fastfilm.core.network.apis.entity

/**
 * {
 *     "sceneId": "场景主键id",
 *     "appearanceId": "当前资产形象ID（参考图id）",
 *     "sceneName": "场景名称",
 *     "scenePrompt": "场景图片描述提示词",
 *     "imageUrl": "场景图片URL",
 *     "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]",
 *     "taskId": "任务ID (雪花算法)"
 * }
 */
data class SimpleSceneInfo(
    /**
     * 场景主键id
     */
    val sceneId: String? = null,
    /**
     * 当前资产形象ID（参考图id）
     */
    var appearanceId: String? = null,
    /**
     * 场景名称
     */
    var sceneName: String? = null,
    /**
     * 场景图片描述提示词
     */
    var scenePrompt: String? = null,
    /**
     * 场景图片URL
     */
    var imageUrl: String? = null,
    /**
     * [枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]
     */
    var genStatus: Int? = null,
    /**
     * 任务ID (雪花算法)
     */
    var taskId: String? = null
) : SimpleAsset {
    override val status: Int?
        get() = genStatus
    override val assetDesc: String?
        get() = scenePrompt
    override val assetName: String?
        get() = sceneName
}