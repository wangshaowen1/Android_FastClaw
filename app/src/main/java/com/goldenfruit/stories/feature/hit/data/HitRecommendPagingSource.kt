package com.kgmedia.fastfilm.feature.hit.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.HitRecommendInfo
import com.kgmedia.fastfilm.core.network.apis.request.HitRecommendPagedBody

class HitRecommendPagingSource(val tabId: String?, val tabType: Int?) : PagingSource<Int, HitRecommendInfo>() {

    override fun getRefreshKey(state: PagingState<Int, HitRecommendInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HitRecommendInfo> {
        return try {
            val pageNum = params.key ?: 1
            val apiResponse = ApiFactory.apiService.hitRecommendList(HitRecommendPagedBody(tabId, tabType, pageNum, 20))
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<HitRecommendInfo> = ArrayList()
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