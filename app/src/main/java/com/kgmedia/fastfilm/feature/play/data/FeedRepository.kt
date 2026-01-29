package com.kgmedia.fastfilm.feature.play.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.core.network.apis.entity.DramaInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

/**
 * 全局单例仓库：确保列表和播放页使用同一个数据流
 */
object FeedRepository {

    // 1. 定义一个全局作用域，用于保持数据流存活
    // 使用 SupervisorJob 确保异常不会导致 Scope 取消
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 2. 缓存当前的 Flow
    private var cachedFeedFlow: Flow<PagingData<DramaInfo>>? = null
    
    // 缓存当前的参数，用于判断是否需要刷新
    private var lastSeriesId: String? = null
    private var lastIsSeriesMode: Boolean? = null

    /**
     * 获取数据流
     * @param forceRefresh 是否强制刷新（例如下拉刷新时传 true）
     */
    fun getFeedFlow(isSeriesMode: Boolean, id: String?, forceRefresh: Boolean = false): Flow<PagingData<DramaInfo>> {
        // 如果参数一致且不强制刷新，直接返回缓存的流（实现数据共享的核心！）
        if (!forceRefresh && cachedFeedFlow != null && lastSeriesId == id && lastIsSeriesMode == isSeriesMode) {
            return cachedFeedFlow!!
        }

        // 创建新的 Pager
        val newFlow = Pager(
            config = PagingConfig(
                pageSize = 10, 
                prefetchDistance = 3, 
                enablePlaceholders = false,
                initialLoadSize = 10 // 保持和 pageSize 一致，防止跳页
            ),
            pagingSourceFactory = {
                // 这里使用你现有的 PagingSource
                VideoFeedPagingSource() 
            }
        ).flow
         .cachedIn(repositoryScope) // [关键] 使用全局 Scope 缓存，使其跨页面存活

        // 更新缓存
        cachedFeedFlow = newFlow
        lastSeriesId = id
        lastIsSeriesMode = isSeriesMode

        return newFlow
    }
    
    /**
     * 清理缓存（可选，比如退出登录或完全退出模块时调用）
     */
    fun clearCache() {
        cachedFeedFlow = null
        lastSeriesId = null
        lastIsSeriesMode = null
    }
}