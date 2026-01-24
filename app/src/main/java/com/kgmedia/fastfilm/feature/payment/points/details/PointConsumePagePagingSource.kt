package com.kgmedia.fastfilm.feature.payment.points.details

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.PointConsumePageInfo
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:获取积分消耗明细分页
 * @Data: 2025/9/10/周三  17:51
 * @Version: 1.0
 * *****************************************
 */
class PointConsumePagePagingSource() : PagingSource<Int, PointConsumePageInfo>() {

    override fun getRefreshKey(state: PagingState<Int, PointConsumePageInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PointConsumePageInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = PagedBody( pageNum, params.loadSize)
            val apiResponse = ApiFactory.apiService.getPointConsumePage(
                body
            )
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items: ArrayList<PointConsumePageInfo> = ArrayList()
                val data = apiResponse.data?.items
                // 加载下一页的键（为 null 表示没有下一页）
                val nextKey =
                    if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
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