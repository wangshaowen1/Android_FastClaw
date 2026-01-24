package com.kgmedia.fastfilm.kit.source.impl

import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.PagedList
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.request.RecommendShortsBody
import com.kgmedia.fastfilm.kit.source.ShortsPageSource


class RecommendShortsPageSource() : ShortsPageSource {
    override suspend fun nextPage(page: Int, pageSize: Int): ApiResponse<PagedList<ShortDramaInfo>?> {
        val response = ApiFactory.apiService.recommendShortList(RecommendShortsBody( RecommendShortsBody.lastRecommendBrowserList ,page, pageSize))
        RecommendShortsBody.lastRecommendBrowserList = response.data?.items?.mapNotNull { it?.dramasId }
        return response
    }
}