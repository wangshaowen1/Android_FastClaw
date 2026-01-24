package com.kgmedia.fastfilm.feature.hit.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.StrategyInfo
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody

class StrategyPagingSource() : PagingSource<Int, StrategyInfo>() {

    override fun getRefreshKey(state: PagingState<Int, StrategyInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, StrategyInfo> {
        return try {
            val pageNum = params.key ?: 1
            val apiResponse = ApiFactory.apiService.strategyList(PagedBody(pageNum, 20))
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<StrategyInfo> = ArrayList()
                val data = apiResponse.data?.items
                // 加载下一页的键（为 null 表示没有下一页）
                val nextKey =
                    if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
                data?.filterNotNull()?.let { items.addAll(it) }
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}