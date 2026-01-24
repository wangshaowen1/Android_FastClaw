package com.kgmedia.fastfilm.feature.series

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.request.DramasSeriesIdPagedBody

class SeriesEpisodePagingSource(private val dramasSeriesId: String?, val totalSize: ((Int) -> Unit)? = null) : PagingSource<Int, ShortDramaInfo>() {

    override fun getRefreshKey(state: PagingState<Int, ShortDramaInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ShortDramaInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = DramasSeriesIdPagedBody(dramasSeriesId, pageNum, params.loadSize)
            val apiResponse = ApiFactory.apiService.seriesShortList(body)
            if (apiResponse.isSuccess) {
                totalSize?.invoke(apiResponse.data?.totalSize ?: 0)
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items = apiResponse.data?.items ?: emptyList()
                val nextKey = if (items.size < params.loadSize) null else pageNum + 1
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}