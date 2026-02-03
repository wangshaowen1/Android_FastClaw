package com.kgmedia.fastfilm.feature.play

import android.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.play.data.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DramasPlayPagerViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "",
        background = Color.TRANSPARENT,
    )

    enum class PlayMode { RECOMMEND, SERIES }
    var currentPlayMode = PlayMode.SERIES

    // 对外暴露的数据流
    private val _feedFlow = MutableStateFlow<PagingData<WorkInfo>>(PagingData.empty())
    val feedFlow: Flow<PagingData<WorkInfo>> = _feedFlow

    init {
        // 默认加载一次，方便进入页面直接看效果
       // loadFeed(true, null)
    }

    fun loadFeed(isSeriesMode: Boolean, id: String?) {
        // 1. 设置当前模式
        currentPlayMode = if (isSeriesMode) PlayMode.SERIES else PlayMode.RECOMMEND

        viewModelScope.launch {
            // Step A: 获取原始 Flow
            val rawFlow = if (isSeriesMode) {
                // [Series模式]：获取专用数据流（内存预加载 或 独立网络请求）
                FeedRepository.getSeriesFlowOrNetwork(id ?: "", true)
            } else {
                // [Feed模式]：获取全局共享流
                // Repository 内部已经用 repositoryScope 缓存过了
                FeedRepository.getFeedFlow(false, id)
            }

            // Step B: 决定是否需要在当前 ViewModel 缓存
            val finalFlow = if (isSeriesMode) {
                // Series 模式下，Flow 可能是新创建的（比如来自 flowOf(PagingData) 或 新的 Pager），
                // 需要绑定到当前 ViewModel 生命周期，防止配置变更（如旋转）丢失状态
                rawFlow.cachedIn(viewModelScope)
            } else {
                // [关键修改] Feed 模式下，直接使用 Repository 的流。
                // 因为 Repository 已经 cachedIn(repositoryScope)，它是全局存活的。
                // 如果这里再 cachedIn(viewModelScope)，会导致 Flow 被包装，可能丢失原有的共享状态。
                rawFlow
            }

            // Step C: 收集并发送给 UI
            finalFlow.collectLatest { pagingData ->
                _feedFlow.emit(pagingData)
            }
        }
    }
}