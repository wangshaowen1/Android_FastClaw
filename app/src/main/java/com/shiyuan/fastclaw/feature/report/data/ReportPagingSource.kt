package com.shiyuan.fastclaw.feature.report.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.response.ReportListItem
import com.shiyuan.fastclaw.core.network.apis.response.toReportInfoList
import okio.IOException

/**
 * @createtime 2026年05月13日 19:42
 * @author TooWhiteT
 */

class ReportPagingSource : PagingSource<Int, ReportInfo>() {
    override fun getRefreshKey(state: PagingState<Int, ReportInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReportInfo> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize
            val response = AppRepository().api.getMyReportList(PagedBody(page, pageSize))
            if (response.data != null) {
                val data = response.data
                val items = data?.toReportInfoList()
                val nextKey = if (items.isNullOrEmpty()) null else page + 1
                items?.let {
                    LoadResult.Page(
                        data = items,
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
