package com.kgmedia.fastfilm.feature.details

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody
import com.kgmedia.fastfilm.core.network.apis.response.ComputingPowerDetailsInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:获取充值明细分页
 * @Data: 2025/9/10/周三  17:51
 * @Version: 1.0
 * *****************************************
 */
class ComputingPowerDetailsPagingSource() : PagingSource<Int, ComputingPowerDetailsInfo>() {

    override fun getRefreshKey(state: PagingState<Int, ComputingPowerDetailsInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComputingPowerDetailsInfo> {
        return try {
            val data = mutableListOf<ComputingPowerDetailsInfo>()
            for (i in 1..50) {
                data.add(
                    ComputingPowerDetailsInfo(
                        "算力充值",
                        detailTime = System.currentTimeMillis(),
                        tradeType = ComputingPowerDetailsInfo.SCORE_TYPE_GET,
                        detailAmount = (100 + i).toLong()
                    )
                )
            }
            LoadResult.Page(data = data, null, null)
//            val pageNum = params.key ?: 1
//            val body = PagedBody(pageNum, params.loadSize)
//            val apiResponse = ApiFactory.apiService.getRechargePage(
//                body
//            )
//            if (apiResponse.isSuccess) {
//                val prevKey = if (pageNum > 1) pageNum - 1 else null
//                val items: ArrayList<ComputingPowerDetailsInfo> = ArrayList()
//                val data = apiResponse.data?.items
//                // 加载下一页的键（为 null 表示没有下一页）
//                val nextKey =
//                    if (data.isNullOrEmpty() || data.size < params.loadSize) null else pageNum + 1
//                data?.filterNotNull()?.let { items.addAll(it) }
//                LoadResult.Page(data = items, prevKey, nextKey)
//            } else {
//                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
//            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}