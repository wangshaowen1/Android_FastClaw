package com.kgmedia.fastfilm.feature.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
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

    override fun onCreate() {
        super.onCreate()
        getData()
    }

    fun getData() {
        launch {
            val mockBanners = listOf(
                BannerInfo(
                    title = "热点话题：我的2026",
                    imageUrl = "https://picsum.photos/id/10/720/360",
                    jumpType = 1,
                    jumpCode = "page_hot_topic",
                    sortNo = 1
                ),
                BannerInfo(
                    title = "AI 智能剧本助手体验",
                    imageUrl = "https://picsum.photos/id/20/720/360",
                    jumpType = 2,
                    jumpCode = "https://ai.kgmedia.com/helper",
                    sortNo = 5
                ),
                BannerInfo(
                    title = "第三方合作活动",
                    imageUrl = "https://picsum.photos/id/30/720/360",
                    jumpType = 3,
                    jumpCode = "https://www.partner-site.com",
                    sortNo = 10
                )
            )
            bannerData.value =mockBanners
            response(mRepo.api.getAppBanner(BannerBody(1))) {
                bannerData.value = this.data?.items
            }
        }
    }

    // 1. 定义 UI 需要观察的数据流
    private val _feedFlow = MutableStateFlow<PagingData<WorkInfo>>(PagingData.empty())
    val feedFlow: Flow<PagingData<WorkInfo>> = _feedFlow

    init {
        loadData("")
    }

    // 2. 加载数据
    fun loadData(seriesId: String) {
        viewModelScope.launch {
            // [关键] 调用单例 FeedRepository 获取带缓存的流
            FeedRepository.getFeedFlow(isSeriesMode = true, id = seriesId)
                .collectLatest { pagingData ->
                    _feedFlow.value = pagingData
                }
        }
    }
}