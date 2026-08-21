package com.shiyuan.fastclaw.feature.mine

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shiyuan.fastclaw.core.network.apis.ApiService
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.response.PayOrderInfo
import com.shiyuan.fastclaw.feature.mine.fragment.MyOrderRecord
import kotlinx.coroutines.CancellationException

class MyOrderRecordPagingSource(
    private val api: ApiService
) : PagingSource<Int, MyOrderRecord>() {

    override fun getRefreshKey(state: PagingState<Int, MyOrderRecord>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MyOrderRecord> {
        return try {
            val requestPage = params.key ?: FIRST_PAGE
            val pageSize = params.loadSize
            val response = api.queryPayOrderList(PagedBody(requestPage, pageSize))
            if (!response.isSuccess) {
                return LoadResult.Error(response.apiException)
            }

            val pageData = response.data
            val items = pageData?.items.orEmpty().map { it.toOrderRecord() }
            val responsePage = pageData?.current?.takeIf { it > 0 } ?: requestPage
            val totalPages = pageData?.pages?.takeIf { it > 0 }
            val nextKey = when {
                totalPages != null -> if (responsePage < totalPages) responsePage + 1 else null
                items.size < pageSize -> null
                else -> responsePage + 1
            }
            LoadResult.Page(
                data = items,
                prevKey = if (responsePage == FIRST_PAGE) null else responsePage - 1,
                nextKey = nextKey
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun PayOrderInfo.toOrderRecord(): MyOrderRecord {
        return MyOrderRecord(
            orderNo = orderNo.orEmpty(),
            title = goodsName.orEmpty(),
            amount = payAmountText.orEmpty(),
            time = createTime.orEmpty()
        )
    }

    companion object {
        private const val FIRST_PAGE = 1
    }
}
