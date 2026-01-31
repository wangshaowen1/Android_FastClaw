package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "assetId": "资产 ID （角色、道具、场景 id）根据业务类型判断，查询单个时入参",
 *     "bizType": "查询单个资产必须要入参业务类型，不入参默认查全部业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]",
 *     "workId": "作品 ID",
 *     "storyId": "剧集 id"
 * }
 */
data class ReferenceImageBody(
    val assetId: String? = null,
    val bizType: Int? = null,
    val storyId: String? = null,
    val workId: String? = null
) : UserTokenBody() {
    companion object {
        const val BIZ_TYPE_ROLE_IMAGE = 41
        const val BIZ_TYPE_PROP_IMAGE = 51
        const val BIZ_TYPE_SCENE_IMAGE = 61
    }
}