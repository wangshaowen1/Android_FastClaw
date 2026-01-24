package com.kgmedia.fastfilm.feature.play.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.kit.source.MediaSourceManager

/**
 * 为选集弹框提供的 Paging3 数据源
 */
class ShortSeriesPagingSource(
    private val mediaSourceManager: MediaSourceManager
) : PagingSource<Int, ShortDramaInfo>() {

    override fun getRefreshKey(state: PagingState<Int, ShortDramaInfo>): Int? {
        // 当 adapter.refresh() 被调用时，这里其实不重要，因为 Paging3 会忽略它并传 null 给 load
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ShortDramaInfo> {
        return try {
            // params.key 为 null 代表是初始化或刷新，默认为第 1 页
            val page = params.key ?: 1

            // ✅ 核心修改：如果是加载第 1 页，强制让 Manager 重置状态
            // 这样无论是“刚进入页面”还是“下拉刷新”，只要 Paging 要第 1 页，Manager 就必须归零
            if (page == 1) {
                mediaSourceManager.resetState(true)
            }
            // 如果是第 1 页，initialData 可能会因为 resetState而被清空，
            // 所以这里直接走网络请求逻辑是安全的

            val newData = mediaSourceManager.loadNextPageAndAwait()

            val prevKey = if (page > 1) page - 1 else null
            val nextKey = if (newData.isNotEmpty() && mediaSourceManager.hasNextPage()) page + 1 else null

            LoadResult.Page(
                data = newData,
                prevKey = prevKey,
                nextKey = nextKey
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }


}