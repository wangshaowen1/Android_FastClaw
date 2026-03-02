package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "workNovelUrl": "小说地址",
 *     "novelCharCount": "小说字符数"
 * }
 */
data class UploadStoryBody(
    val novelCharCount: Int? = null,
    val workNovelUrl: String? = null,
    val workTitle: String? = null,
    val totalEpisodeCntCode: String? = null,
    val totalEpisodeCnt: String? = null,
    val workRatio: String? = null,
    val workStyleCode: String? = null,
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
) {
    fun checkStatus(): Boolean {
        return genStatus != 4
    }
}

/**
 * {
 *         "workId": "作品ID",
 *         "genStatus": "[枚举]生成状态 [0=待生成 1=生成中 2=成功 3=失败 4=算力不足，请充值]",
 *         "result": "生成结果（作品梗概内容）"
 *     }
 */
data class RollNovelSummaryData(
    val genStatus: Int? = null,
    val result: RollNovelSummaryResult? = null,
    val workId: String? = null
) {
    fun isSuccess(): Boolean {
        return genStatus == 2
    }

    fun isFail(): Boolean {
        return genStatus == 3
    }
}

data class RollNovelSummaryResult(
    val workDesc: String? = null,
    val workTitle: String? = null,
    val structureList: List<RollNovelStructure>? = null,
)

/**
 * {
 *             "workTitle": "作品名称",
 *             "workDesc": "作品简介",
 *             "structureList": [
 *                 {
 *                     "id": "主键ID",
 *                     "storyId": "剧集ID",
 *                     "storyTitle": "剧集标题",
 *                     "storySummary": "剧集梗概"
 *                 }
 *             ]
 *         }
 */
data class RollNovelStructure(
    val id: String? = null,
    val storyId: String? = null,
    val storySummary: String? = null,
    val storyTitle: String? = null
)