package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "workTitle": "作品名称",
 *     "workStyleCode": "作品风格 code",
 *     "workRatio": "视频比例code：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)",
 *     "totalEpisodeCnt": "自定义集数",
 *     "totalEpisodeCntCode": "剧集集数code：1-15=RANGE_1_15  15-30=RANGE_15_30  自定义=CUSTOM_EXACT",
 *     "storyStructureStr": "故事剧集梗概字符串"
 * }
 */
data class CreateWorkBody(
    val workTitle: String? = null,
    val storyStructureStr: String? = null,
    val totalEpisodeCntCode: String? = null,
    val totalEpisodeCnt: String? = null,
    val workRatio: String? = null,
    val workStyleCode: String? = null,
) : UserTokenBody()

data class CreateStoryWorkBody(
    val workTitle: String? = null,
    val storyContent: String? = null,//剧集剧本内容
    val totalEpisodeCntCode: String? = null,
    val totalEpisodeCnt: String? = null,
    val workRatio: String? = null,
    val workStyleCode: String? = null,
) : UserTokenBody()

/**
 * {
 *     "workTitle": "作品名称",
 *     "workDesc": "作品简介",
 *     "workType": "作品类型：1-灵感创作 2-小说",
 *     "storyContent": "剧集剧本内容",
 *     "storyId": "剧集 id",
 *     "workId": "作品 ID"
 * }
 */
data class SaveEpisodeScriptBody(
    val storyContent: String? = null,
    val workId: String? = null,
    val storyId: String? = null,
    val workType: Int? = null
)