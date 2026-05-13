package com.shiyuan.fastclaw.feature.mine

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.network.apis.ApiService
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.response.UserPointItemData
import okio.IOException

/**
 * @createtime 2026年05月13日 19:42
 * @author TooWhiteT
 */

class UserPointPagingSource : PagingSource<Int, UserPointItemData>() {
    override fun getRefreshKey(state: PagingState<Int, UserPointItemData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserPointItemData> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            val response = AppRepository().api.queryUserPointsPage(PagedBody(page, pageSize))
            if (response.data != null) {
                val data = response.data.records
                val nextKey = if (data.isNullOrEmpty()) null else page + 1
                data?.let {
                    LoadResult.Page(
                        data = data,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = nextKey
                    )
                } ?: run {
                    LoadResult.Error(IOException("data is null"))
                }
            } else {
                LoadResult.Error(IOException("data is null"))
            }

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}