package com.kgmedia.fastfilm.feature.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.core.network.apis.entity.HitRecommendInfo
import com.kgmedia.fastfilm.core.network.apis.entity.DramaInfo
import com.kgmedia.fastfilm.core.network.apis.request.BannerBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.home.data.HomePagingSource
import com.kgmedia.fastfilm.feature.play.data.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class HomeViewModel : AppRepositoryViewModel<AppRepository>() {

    val bannerData = MutableLiveData<List<BannerInfo>>()

    override fun onCreate() {
        super.onCreate()
        getData()
    }

    fun getData() {
        launch {
            response(mRepo.api.getAppBanner(BannerBody(1))) {
                bannerData.value = this.data?.items
            }
        }
    }

    val dataGet: Flow<PagingData<HitRecommendInfo>> = Pager(
        config = PagingConfig(initialLoadSize = 20, pageSize = 20),
        pagingSourceFactory = { HomePagingSource() }
    ).flow.cachedIn(viewModelScope)

    // 1. 定义 UI 需要观察的数据流
    private val _feedFlow = MutableStateFlow<PagingData<DramaInfo>>(PagingData.empty())
    val feedFlow: Flow<PagingData<DramaInfo>> = _feedFlow

    init {
        loadData("")
    }

    // 2. 加载数据
    fun loadData(seriesId: String) {
        viewModelScope.launch {
            // [关键] 调用单例 FeedRepository 获取带缓存的流
            FeedRepository.getFeedFlow(isSeriesMode = true, id = seriesId)
                .cachedIn(viewModelScope) // 绑定到当前 ViewModel 生命周期
                .collectLatest { pagingData ->
                    _feedFlow.value = pagingData
                }
        }
    }
}