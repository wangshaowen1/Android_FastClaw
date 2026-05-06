package com.shiyuan.fastclaw.feature.home

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.EntityCache
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.network.apis.entity.BannerInfo
import com.shiyuan.fastclaw.core.network.apis.entity.WorkInfo
import com.shiyuan.fastclaw.core.network.apis.request.BannerBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.play.data.FeedRepository
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