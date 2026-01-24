package com.kgmedia.fastfilm.feature.invitation.income

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody
import com.kgmedia.fastfilm.core.network.apis.response.InviteIncomePageInfo
import com.nhsd.basic.bus.FlowEventBus

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:邀请收益列表
 * @Data: 2025/7/14/周一  10:24
 * @Version: 1.0
 * *****************************************
 */
class InvitationIncomePagingSource() : PagingSource<Int, InviteIncomePageInfo>() {

    override fun getRefreshKey(state: PagingState<Int, InviteIncomePageInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, InviteIncomePageInfo> {
        return try {
            val pageNum = params.key ?: 1
            val apiResponse = ApiFactory.apiService.getInviteIncomePage(
                PagedBody(
                    pageNum, params.loadSize
                )
            )
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<InviteIncomePageInfo> = ArrayList()
                val data = apiResponse.data?.items
                //todo FlowEventBus.post(Events.InvitationIncomeEvent(apiResponse.data?.totalAmount?:-100))
                // 加载下一页的键（为 null 表示没有下一页）
                val nextKey = if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
                data?.filterNotNull()?.let { items.addAll(it) }
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}