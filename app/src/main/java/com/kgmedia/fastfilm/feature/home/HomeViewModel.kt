package com.kgmedia.fastfilm.feature.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.EntityCache
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.request.BannerBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.play.data.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class HomeViewModel : AppRepositoryViewModel<AppRepository>() {

    val bannerData = MutableLiveData<List<BannerInfo>>()
    private val bannerCache: EntityCache<List<BannerInfo>> by lazy {
        object : EntityCache<List<BannerInfo>>("HomeBannerList") {}
    }

    override fun onCreate() {
        super.onCreate()
        getData()
    }

    fun getData() {
        launch({
            response(mRepo.api.getAppBanner(BannerBody(BannerBody.LOCATION_HOME)), {
                bannerData.value = this.data
                bannerCache.cache(this.data)
            }) {
                bannerCache.cache?.let {
                    bannerData.value = it
                }
            }
        }) {
            bannerCache.cache?.let {
                bannerData.value = it
            }
        }
    }

    // 1. 定义 UI 需要观察的数据流
    private val _feedFlow = MutableStateFlow<PagingData<WorkInfo>>(PagingData.empty())
    val feedFlow: Flow<PagingData<WorkInfo>> = _feedFlow

    init {
        loadData()
    }

    // 2. 加载数据
    fun loadData() {
        viewModelScope.launch {
            // [关键] 调用单例 FeedRepository 获取带缓存的流
            FeedRepository.getFeedFlow(isSeriesMode = false, null)
                .collectLatest { pagingData ->
                    _feedFlow.value = pagingData
                }
        }
    }
}