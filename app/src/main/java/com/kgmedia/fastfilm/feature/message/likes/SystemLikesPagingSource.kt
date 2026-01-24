package com.kgmedia.fastfilm.feature.message.likes

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageLikeInfo
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody

class SystemLikesPagingSource : PagingSource<Int, SystemMessageLikeInfo>() {
    override fun getRefreshKey(state: PagingState<Int, SystemMessageLikeInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SystemMessageLikeInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = PagedBody(pageNum, params.loadSize)
            val apiResponse = ApiFactory.apiService.messagesLikes(body)
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val nextKey = if (apiResponse.data?.items.isNullOrEmpty()) null else pageNum + 1
                val items = apiResponse.data?.items ?: emptyList()
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}