package com.shiyuan.fastclaw.feature.mine

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shiyuan.fastclaw.core.network.apis.ApiService
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.response.toInterviewReportInfoList
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import java.io.IOException
import kotlinx.coroutines.CancellationException

class InterviewReportPagingSource(
    private val api: ApiService,
) : PagingSource<Int, ReportInfo>() {

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
            val response = api.getInterviewReportList(PagedBody(page, pageSize))
            if (!response.isSuccess) {
                return LoadResult.Error(response.apiException)
            }
            val items = response.data?.toInterviewReportInfoList()
                ?: return LoadResult.Error(IOException("data is null"))
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (items.size < pageSize) null else page + 1
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
