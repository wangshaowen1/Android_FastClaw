package com.kgmedia.fastfilm.feature.mine.episode

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.MyTvSeriesListInfo
import com.kgmedia.fastfilm.core.network.apis.request.MyTvSeriesBody

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:剧集
 * @Data: 2025/7/14/周一  10:24
 * @Version: 1.0
 * *****************************************
 */
class EpisodeListPagingSource(
    private val listingType: Int,
    private val onTotalCountUpdate: (Int) -> Unit = {}
) : PagingSource<Int, MyTvSeriesListInfo>() {

    override fun getRefreshKey(state: PagingState<Int, MyTvSeriesListInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MyTvSeriesListInfo> {
        return try {
            val pageNum = params.key ?: 1
            val apiResponse = ApiFactory.apiService.myTvSeriesList(
                MyTvSeriesBody(
                    launchStatus = if (listingType == EpisodeListFragment.LISTING_TYPE_LAUNCH) {
                        1
                    } else {
                        0
                    },
                    pageNum = pageNum,
                    pageSize = params.loadSize
                )
            )
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<MyTvSeriesListInfo> = ArrayList()
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