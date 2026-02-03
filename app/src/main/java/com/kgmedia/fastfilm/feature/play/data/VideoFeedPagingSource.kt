package com.kgmedia.fastfilm.feature.play.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import kotlinx.coroutines.delay

class VideoFeedPagingSource : PagingSource<Int, WorkInfo>() {
    override fun getRefreshKey(state: PagingState<Int, WorkInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WorkInfo> {
        return try {
            val pageNum = params.key ?: 1

            // 模拟网络延迟 1秒，这就很真实
            delay(1000)

            // --- ⚠️ 调试模式：使用假数据 ---
            val items = FakeDataSource.getFakeData(pageNum)
            // ---------------------------

            // 如果使用真实接口，把上面那行注释掉，解开下面的：
//            val body = PagedBody(pageNum, params.loadSize)
//            val apiResponse = ApiFactory.apiService.recommendDramaList(body)
//            val items = if (apiResponse.isSuccess) apiResponse.data?.items ?: emptyList() else emptyList()

            val prevKey = if (pageNum > 1) pageNum - 1 else null
            val nextKey = if (items.isNotEmpty()) pageNum + 1 else null

            LoadResult.Page(data = items, prevKey = prevKey, nextKey = nextKey)

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}