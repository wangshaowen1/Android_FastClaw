package com.kgmedia.fastfilm.core.network.apis.request

class DramasSeriesIdPagedBody(
    /**
     * 剧集Id，接受Long和String类型
     */
    val dramasSeriesId: String?,
    pageNum: Int,
    pageSize: Int,
) : PagedBody(pageNum, pageSize)