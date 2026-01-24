package com.kgmedia.fastfilm.feature.play

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * 短剧item VM
 */
class ItemSeriesShortVM(info: ShortDramaInfo?, val position: Int, private val eventVm: ItemEventsViewModel<ShortClickEvent>) : AppItemViewModel<ShortDramaInfo>(info) {
    val playing by lazy { ObservableBoolean(false) }



    fun durationText(): String? {
        return DateUtil.parseMMSSString(data.get()?.getDurationMs()?.toInt()?:0)
    }

    fun playCountText(): String {
        return data.get()?.interaction?.playNumStr.toString()
    }

    fun onItemClick(){
        eventVm.postEvent(ShortClickEvent.ItemClick(data.get(),position))
    }
}

