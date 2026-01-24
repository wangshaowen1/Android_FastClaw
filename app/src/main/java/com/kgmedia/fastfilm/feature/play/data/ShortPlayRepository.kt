package com.kgmedia.fastfilm.feature.play.data

import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.request.DramasIdBody
import com.kgmedia.fastfilm.core.network.apis.request.DramasSeriesId
import com.kgmedia.fastfilm.core.network.apis.request.SeriesFollowBody
import com.kgmedia.fastfilm.core.network.apis.request.ShortLikeBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShortPlayRepository : AppRepository() {


    suspend fun getUnreleasedShortDramaDetail(dramaId: String?) = withContext(Dispatchers.IO) {
        api.shortDramasInfo(DramasIdBody(dramaId))
    }
    suspend fun getShortDramaDetail(dramaId: String?) = withContext(Dispatchers.IO) {
        api.releasedShortDramasInfo(DramasIdBody(dramaId))
    }

    suspend fun deleteShortDrama(dramaId: String?) = withContext(Dispatchers.IO) {
        api.deleteDramaInfo(DramasIdBody(dramaId))
    }

    suspend fun unlockShortDrama(dramaId: String?, dramaSeriesId: String?) = withContext(Dispatchers.IO) {
        api.unlockShortDrama(DramasSeriesId(dramaSeriesId, dramaId))
    }

    suspend fun autoUnlockSeries(dramaSeriesId: String?, auto: Boolean?) = withContext(Dispatchers.IO) {
        api.autoUnlockSwitch(DramasSeriesId(dramaSeriesId, autoUnlockStatus = if (auto == true) 1 else 0))
    }

    /**
     * 追剧或取消追剧
     * @param seriesId 剧集ID
     * @param followStatus 追剧状态 0-取消追剧 1-追剧
     */
    suspend fun seriesFollow(seriesId: String?, followStatus: Int) = withContext(Dispatchers.IO) {
        api.seriesFollow(SeriesFollowBody(seriesId, followStatus))
    }

    /**
     * 点赞剧本
     * @param dramaId 短剧id，接受Long和String类型
     */
    suspend fun likeShortDrama(dramaId: String?) = withContext(Dispatchers.IO) {
        api.likeShortDrama(ShortLikeBody(dramaId))
    }

    /**
     * 发布剧本
     * @param dramaId 短剧id，接受Long和String类型
     */
    suspend fun publishShortDrama(dramaId: String?) = withContext(Dispatchers.IO) {
        api.publishShortDrama(DramasIdBody(dramaId))
    }
}