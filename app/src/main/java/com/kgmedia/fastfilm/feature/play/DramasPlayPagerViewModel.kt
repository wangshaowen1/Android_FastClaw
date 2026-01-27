package com.kgmedia.fastfilm.feature.play

import android.graphics.Color
import androidx.databinding.ObservableFloat
import androidx.lifecycle.MutableLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.TrackInfo
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.getResColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DramasPlayPagerViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "",
        titleColor = R.color.textColorPrimary.getResColor(),
        background = Color.TRANSPARENT,
    )

    // 播放模式：推荐流(无限) vs 剧集流(自动连播)
    enum class PlayMode { RECOMMEND, SERIES }

    var currentPlayMode = PlayMode.RECOMMEND
    private var currentSeriesId: String? = null

    // 数据流
    private val _feedFlow = MutableStateFlow<PagingData<TrackInfo>>(PagingData.empty())
    val feedFlow: Flow<PagingData<TrackInfo>> = _feedFlow

    /**
     * 初始化数据源
     * @param isSeriesMode 是否是剧集模式
     * @param id 系列ID 或 推荐种子ID
     */
    fun loadFeed(isSeriesMode: Boolean, id: String?) {
        currentPlayMode = if (isSeriesMode) PlayMode.SERIES else PlayMode.RECOMMEND
        currentSeriesId = id

        // 创建 PagingSource
//        val pager = Pager(
//            config = PagingConfig(pageSize = 10, prefetchDistance = 3, enablePlaceholders = false),
//            pagingSourceFactory = {
//                if (isSeriesMode) {
//                    // 剧集 PagingSource (需要你自己实现)
//                    repository.getSeriesPagingSource(id!!)
//                } else {
//                    // 推荐 PagingSource (需要你自己实现)
//                    repository.getRecommendPagingSource()
//                }
//            }
//        )

        // 绑定生命周期
        // 这里只是示例，实际上你应该把 Flow 暴露出去
        // _feedFlow.emitAll(pager.flow.cachedIn(viewModelScope))
    }

}