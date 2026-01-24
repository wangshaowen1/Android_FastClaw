package com.kgmedia.fastfilm.feature.hit

import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.DanmakuItem
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramasStatisticsData
import com.kgmedia.fastfilm.core.network.apis.request.DanmakuBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response


class HitViewModel : AppRepositoryViewModel<AppRepository>() {

    val statisticsData = MutableLiveData<ShortDramasStatisticsData>()
    val danmakuList = MutableLiveData<List<DanmakuItem>>()

    override fun onCreate() {
        super.onCreate()
        getData()
        getDanmakuList()
    }

    fun getData() {
        launch {
            response(mRepo.api.shortDramaStatistics(UserTokenBody())) {
                statisticsData.value = this.data
            }
        }
    }

    fun getDanmakuList() {
        launch {
            response(mRepo.api.danmakuList(DanmakuBody(2, 1, 30))) {
                danmakuList.value = this.data?.items
            }
        }
    }
}