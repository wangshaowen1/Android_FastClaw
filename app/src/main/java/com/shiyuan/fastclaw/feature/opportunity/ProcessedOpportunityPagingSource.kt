package com.shiyuan.fastclaw.feature.opportunity

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shiyuan.fastclaw.core.network.apis.ApiService
import com.shiyuan.fastclaw.core.network.apis.request.OpportunityProcessedPageBody
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityProcessedItemData
import java.io.IOException
import kotlinx.coroutines.CancellationException

/**
 * 按处理状态分页加载机会。
 *
 * 请求：[OpportunityProcessedPageBody]，包含状态、页码和页大小。
 * 返回：接口响应中的 [OpportunityProcessedItemData] 列表，并转换为 Paging3 页结果。
 *
 * @param status 处理状态，仅使用 COMMUNICATING 或 UNSUITABLE。
 */
class ProcessedOpportunityPagingSource(
    private val api: ApiService,
    private val status: String,
) : PagingSource<Int, OpportunityProcessedItemData>() {

    /** 下拉刷新固定从第一页重新加载。 */
    override fun getRefreshKey(state: PagingState<Int, OpportunityProcessedItemData>): Int? = null

    /**
     * 加载指定页的已处理机会。
     *
     * 业务失败和空分页数据转换为 [LoadResult.Error]；成功时根据服务端分页信息计算前后页键。
     */
    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, OpportunityProcessedItemData> {
        return try {
            val pageNum = params.key ?: 1
            val pageSize = params.loadSize.coerceIn(1, MAX_PAGE_SIZE)
            val response = api.queryProcessedOpportunityPage(
                OpportunityProcessedPageBody(
                    status = status,
                    pageNum = pageNum,
                    pageSize = pageSize
                )
            )
            if (!response.isSuccess) {
                return LoadResult.Error(response.apiException)
            }

            val page = response.data?.opportunities
                ?: return LoadResult.Error(IOException("data is null"))
            val responsePage = page.pageNum.toIntOrNull() ?: pageNum
            val totalPage = page.totalPage.toIntOrNull()
            val nextKey = if (totalPage != null) {
                if (responsePage >= totalPage) null else responsePage + 1
            } else {
                if (page.items.size < pageSize) null else responsePage + 1
            }
            LoadResult.Page(
                data = page.items,
                prevKey = if (responsePage == 1) null else responsePage - 1,
                nextKey = nextKey
            )
        } catch (e: CancellationException) {
            // 协程取消必须继续向上抛出，避免 Paging 误判为普通加载失败。
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private companion object {
        const val MAX_PAGE_SIZE = 50
    }
}
