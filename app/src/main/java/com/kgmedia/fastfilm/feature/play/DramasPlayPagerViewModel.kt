package com.kgmedia.fastfilm.feature.play

import android.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
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

    /**
     * 加载数据
     */
    fun loadFeed(isSeriesMode: Boolean, id: String?) {
        currentPlayMode = if (isSeriesMode) PlayMode.SERIES else PlayMode.RECOMMEND

        viewModelScope.launch {
            // [修改] 从单例仓库获取数据流
            // 注意：这里再次 cachedIn(viewModelScope) 是为了绑定 UI 生命周期，
            // 但底层数据已经是被 Repository 缓存过的，不会触发网络请求。
            FeedRepository.getFeedFlow(isSeriesMode, id)
                .collectLatest {
                    _feedFlow.value = it
                }
        }

//        // 1. 创建 Pager
//        val pager = Pager(
//            config = PagingConfig(pageSize = 10, prefetchDistance = 3, enablePlaceholders = false),
//            pagingSourceFactory = {
//                // 直接使用我们写的假数据 Source
//                VideoFeedPagingSource()
//            }
//        )
//
//        // 2. 收集数据并发射给 _feedFlow
//        viewModelScope.launch {
//            pager.flow.cachedIn(viewModelScope).collectLatest {
//                _feedFlow.value = it
//            }
//        }
    }
}