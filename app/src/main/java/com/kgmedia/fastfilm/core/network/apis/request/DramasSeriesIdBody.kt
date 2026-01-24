package com.kgmedia.fastfilm.core.network.apis.request

data class DramasSeriesIdBody(
    /**
     * 剧集Id，接受Long和String类型
     */
    val dramasSeriesId: String?
) : UserTokenBody()

data class ContinueGenerateBody(
    /**
     * 剧集Id，接受Long和String类型
     */
    val dramasSeriesId: String?,
    val inputContent: String?//输入内容
) : UserTokenBody()