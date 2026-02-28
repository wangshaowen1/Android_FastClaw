package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "workNovelUrl": "小说地址",
 *     "novelCharCount": "小说字符数"
 * }
 */
data class UploadStoryBody(
    val novelCharCount: Int? = null,
    val workNovelUrl: String? = null
) : UserTokenBody()

/**
 *  {
 *         "workId": "作品ID",
 *         "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败 4=算力不足，请充值]"
 *     }
 */
data class UploadStoryData(
    val genStatus: Int? = null,
    val workId: String? = null
)