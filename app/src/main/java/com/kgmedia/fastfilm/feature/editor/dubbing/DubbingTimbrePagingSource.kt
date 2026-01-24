package com.kgmedia.fastfilm.feature.editor.dubbing

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.DramasTimbreInfo
import com.kgmedia.fastfilm.core.network.apis.request.DramasTimbrePagedBody

class DubbingTimbrePagingSource(private val dramasId: String?, private val timbreId: String?) : PagingSource<Int, DramasTimbreInfo>() {

    override fun getRefreshKey(state: PagingState<Int, DramasTimbreInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DramasTimbreInfo> {
        return try {
            val pageNum = params.key ?: 1
            val body = DramasTimbrePagedBody(dramasId, timbreId, pageNum, params.loadSize)
            val apiResponse = ApiFactory.apiService.dubbingVoiceList(body)
            if (apiResponse.isSuccess) {
                val prevKey = if (pageNum > 1) pageNum - 1 else null
                val items = apiResponse.data?.items ?: emptyList<DramasTimbreInfo>()
                val nextKey = if (items.size < params.loadSize) null else pageNum + 1
                LoadResult.Page(data = items, prevKey, nextKey)
            } else {
                LoadResult.Error(ApiException(apiResponse.code, apiResponse.msg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}