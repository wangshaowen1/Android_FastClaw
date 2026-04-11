package com.kgmedia.fastfilm.feature.home

import androidx.lifecycle.MutableLiveData
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

    // FeedRepository 已用 cachedIn(repositoryScope) 做全局缓存，直接暴露即可
    val feedFlow: Flow<PagingData<WorkInfo>> = FeedRepository.getFeedFlow(isSeriesMode = false, null)
}