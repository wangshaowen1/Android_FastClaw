package com.kgmedia.fastfilm.feature.play

import android.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.TrackInfo
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.play.data.VideoFeedPagingSource
import com.nhsd.basic.ext.getResColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DramasPlayPagerViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "",
        titleColor = R.color.textColorPrimary.getResColor(),
        background = Color.TRANSPARENT,
    )

    enum class PlayMode { RECOMMEND, SERIES }
    var currentPlayMode = PlayMode.RECOMMEND

    // 对外暴露的数据流
    private val _feedFlow = MutableStateFlow<PagingData<TrackInfo>>(PagingData.empty())
    val feedFlow: Flow<PagingData<TrackInfo>> = _feedFlow

    init {
        // 默认加载一次，方便进入页面直接看效果
        loadFeed(false, null)
    }

    /**
     * 加载数据
     */
    fun loadFeed(isSeriesMode: Boolean, id: String?) {
        currentPlayMode = if (isSeriesMode) PlayMode.SERIES else PlayMode.RECOMMEND

        // 1. 创建 Pager
        val pager = Pager(
            config = PagingConfig(pageSize = 10, prefetchDistance = 3, enablePlaceholders = false),
            pagingSourceFactory = {
                // 直接使用我们写的假数据 Source
                VideoFeedPagingSource()
            }
        )

        // 2. 收集数据并发射给 _feedFlow
        viewModelScope.launch {
            pager.flow.cachedIn(viewModelScope).collectLatest {
                _feedFlow.value = it
            }
        }
    }
}