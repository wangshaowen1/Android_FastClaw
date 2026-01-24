package com.kgmedia.fastfilm.feature.mine.chase

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.TheTvSeriesIFollowInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:追剧
 * @Data: 2025/11/4/周二  13:47
 * @Version: 1.0
 * *****************************************
 */
class ItemBingeWatchVm (
    private val eventVm: ItemEventsViewModel<BingeWatchEvent>,
    theTvSeriesIFollowInfo: TheTvSeriesIFollowInfo?
) : AppItemViewModel<TheTvSeriesIFollowInfo>(theTvSeriesIFollowInfo){
    fun onItemClick() {
        eventVm.postEvent(BingeWatchEvent.ItemClick(data.get()))
    }
}