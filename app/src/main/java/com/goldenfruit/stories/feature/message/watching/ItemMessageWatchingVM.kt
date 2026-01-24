package com.kgmedia.fastfilm.feature.message.watching

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageWatchingInfo
import com.kgmedia.fastfilm.feature.series.SeriesActivity

/**
 * 消息item
 */
class ItemMessageWatchingVM(info: SystemMessageWatchingInfo) :
    AppItemViewModel<SystemMessageWatchingInfo>(info) {
    fun onClick() {
        SeriesActivity.start(data.get()?.dramasSeriesId)
    }
}