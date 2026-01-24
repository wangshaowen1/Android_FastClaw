package com.kgmedia.fastfilm.feature.series

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * 剧集-item
 */
class ItemSeriesEpisodeVM(private val eventVm: ItemEventsViewModel<SeriesEpisodeEvent>, private val position: Int, data: ShortDramaInfo?) : AppItemViewModel<ShortDramaInfo>(data) {
    fun onClick() {
        eventVm.postEvent(SeriesEpisodeEvent.Click(position, this))
    }
}