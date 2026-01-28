package com.kgmedia.fastfilm.feature.home.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.HitRecommendInfo
import com.kgmedia.fastfilm.core.network.apis.request.HitRecommendPagedBody

class HomePagingSource() : PagingSource<Int, HitRecommendInfo>() {

    override fun getRefreshKey(state: PagingState<Int, HitRecommendInfo>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HitRecommendInfo> {
        return try {
            val pageNum = params.key ?: 1
            // 在 HomePagingSource 的 load 方法中临时测试
            val mockItems = arrayListOf<HitRecommendInfo>()
            for (i in 1..20) {
                mockItems.add(
                    HitRecommendInfo(
                        popularRecommendDramasId = "${pageNum}_$i",
                        dramasName = "测试剧集 ${pageNum}-$i",
                        dramasCover = "https://picsum.photos/200/300?random=$i",
                        tags = listOf("热门", "短剧"),
                        introduction = "这是一段自动生成的简介，当前是第 $pageNum 页。",
                        vipStatus = if (i % 2 == 0) 1 else 0
                    )
                )
            }
// 直接返回成功结果
            return LoadResult.Page(data = mockItems, prevKey = null, nextKey = pageNum + 1)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}