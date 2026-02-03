package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "bizType": "业务类型枚举41=角色图片 51=道具图片  61=场景图片",
 *     "prompt": "提示词",
 *     "assetId": "资产 ID （角色、道具、场景 id）根据业务类型判断",
 *     "referenceImageId": "参考图 id",
 *     "referenceImageInStatus": "参考图片入镜状态(0-不入镜，1-入镜)",
 *     "referenceImageUrl": "参考图片 URL",
 *     "storyId": "剧集 id",
 *     "workId": "作品 ID"
 * }
 */
data class GenerateAssetBody(
    /**
     * 业务类型枚举41=角色图片 51=道具图片  61=场景图片
     */
    val bizType: Int?,
    /**
     * 提示词
     */
    val prompt: String?,
    /**
     * 资产 ID （角色、道具、场景 id）根据业务类型判断，查询单个时入参
     */
    val assetId: String?,
    /**
     * 参考图 id
     */
    val referenceImageId: String?,
    /**
     * 参考图片入镜状态(0-不入镜，1-入镜)
     */
    val referenceImageInStatus: Int?,
    /**
     * 参考图片 URL
     */
    val referenceImageUrl: String?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    /**
     * 作品 ID
     */
    val workId: String?,
)