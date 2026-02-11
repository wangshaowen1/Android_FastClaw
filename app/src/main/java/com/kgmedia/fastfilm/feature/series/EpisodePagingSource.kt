package com.kgmedia.fastfilm.feature.series

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.request.WorkIdPagedBody
import kotlin.math.min

/**
 * @param requiredSize 需要显示剧集数量，不足的用空剧集填充
 */
class EpisodePagingSource(private val workId: String?, private val requiredSize: Int = 0) : PagingSource<Int, EpisodeInfo>() {

    /**
     * 已加载剧集数量
     */
    private var loadSize = 0

    override fun getRefreshKey(state: PagingState<Int, EpisodeInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EpisodeInfo> {
        return try {
            val pageNum = params.key ?: 1
            if (pageNum <= 1) {
                loadSize = 0
            }
            val body = WorkIdPagedBody(workId, pageNum, params.loadSize)
            val apiResponse = ApiFactory.apiService.episodeList(body)
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items = arrayListOf<EpisodeInfo>()
                apiResponse.data?.items?.let { items.addAll(it) }
                if (requiredSize > 0 && items.size < params.loadSize) {
                    val blankSize = min(requiredSize - loadSize - items.size, params.loadSize - items.size)
                    if (blankSize > 0) {
                        items.addAll(List(blankSize) { EpisodeInfo() })
                    }
                }
                loadSize += items.size
                val nextKey = if (requiredSize > 0) {
                    if (loadSize >= requiredSize) null else pageNum + 1
                } else {
                    if (items.size < params.loadSize) null else pageNum + 1
                }
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}