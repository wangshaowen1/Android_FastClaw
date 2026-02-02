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
    val sceneId: String?,
    /**
     * 当前资产形象ID（参考图id）
     */
    val appearanceId: String?,
    /**
     * 场景名称
     */
    val sceneName: String?,
    /**
     * 场景图片描述提示词
     */
    val scenePrompt: String?,
    /**
     * 场景图片URL
     */
    val imageUrl: String?,
    /**
     * [枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]
     */
    val genStatus: Int?,
    /**
     * 任务ID (雪花算法)
     */
    val taskId: String?
) : SimpleAsset {
    override val assetDesc: String?
        get() = scenePrompt
    override val assetName: String?
        get() = sceneName
}