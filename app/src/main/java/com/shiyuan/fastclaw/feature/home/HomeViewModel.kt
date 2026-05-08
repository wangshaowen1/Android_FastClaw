package com.shiyuan.fastclaw.feature.home

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import com.shiyuan.fastclaw.R
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

    val userInfo by lazy { ObservableField<Map<String, String?>>() }
    
    val greetingText = ObservableField<String>("嗨，我是你的专属职业经纪人！\n有什么可以帮到你的吗？")
    
    val loadState = ObservableField<Int>(0) // 0: 正常, 1: 加载中, 2: 空, 3: 错误


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
    
    inner class ViewClickHandler {
        fun onClick(view: android.view.View) {
            when (view.id) {

            }
        }
    }
}