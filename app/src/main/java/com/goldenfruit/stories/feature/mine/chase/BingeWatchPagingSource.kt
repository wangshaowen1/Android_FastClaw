package com.kgmedia.fastfilm.feature.mine.chase

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.TheTvSeriesIFollowInfo
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:追剧
 * @Data: 2025/7/14/周一  10:24
 * @Version: 1.0
 * *****************************************
 */
class BingeWatchPagingSource(private val onTotalCountUpdate: (Int) -> Unit = {}) :
    PagingSource<Int, TheTvSeriesIFollowInfo>() {

    override fun getRefreshKey(state: PagingState<Int, TheTvSeriesIFollowInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TheTvSeriesIFollowInfo> {
        return try {
            val pageNum = params.key ?: 1
            val apiResponse = ApiFactory.apiService.theTvSeriesIFollow(
                PagedBody(
                    pageNum, params.loadSize
                )
            )
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<TheTvSeriesIFollowInfo> = ArrayList()
                val data = apiResponse.data?.items
                // 加载下一页的键（为 null 表示没有下一页）
                val nextKey =
                    if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
                data?.filterNotNull()?.let { items.addAll(it) }
                onTotalCountUpdate.invoke(apiResponse.data?.totalSize ?: 0)
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}