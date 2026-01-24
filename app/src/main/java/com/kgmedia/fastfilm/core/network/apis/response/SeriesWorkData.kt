package com.kgmedia.fastfilm.core.network.apis.response

/**
 *  {
 *         "dramasId": "短剧id，接受Long和String类型",
 *         "dramasName": "短剧名称",
 *         "workNo": "短剧作品所在剧集中的集数编号 从1开始",
 *         "scriptIntro": "剧本简介",
 *         "dramasCoverUrl": "短剧封面URL"
 *     }
 */
data class SeriesWorkData(
    val dramasCoverUrl: String? = null,
    val dramasId: String? = null,
    val dramasName: String? = null,
    val scriptIntro: String? = null,
    val styleId: String? = null,//短剧风格表id(b_short_dramas_style)
    val videoRatio: Int? = null,//[枚举]视频比例 [1=9:16 2=16:9 3=3:4 4=4:3]
    val workNo: Int? = null
)