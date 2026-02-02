package com.kgmedia.fastfilm.core.network.apis.entity

/**
 * {
 *     "propId": "道具主键 id",
 *     "appearanceId": "当前资产形象ID（参考图id）",
 *     "propName": "道具名称",
 *     "propPrompt": "道具图片描述提示词",
 *     "imageUrl": "道具图片URL",
 *     "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败]",
 *     "taskId": "任务ID (雪花算法)"
 * }
 */
data class SimplePropInfo(
    /**
     * 道具主键 id
     */
    val propId: String?,
    /**
     * 当前资产形象ID（参考图id）
     */
    val appearanceId: String?,
    /**
     * 道具名称
     */
    val propName: String?,
    /**
     * 道具图片描述提示词
     */
    val propPrompt: String?,
    /**
     * 道具图片URL
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
        get() = propPrompt
    override val assetName: String?
        get() = propName
}