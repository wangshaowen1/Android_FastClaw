package com.kgmedia.fastfilm.feature.editor.dubbing

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.DramasTimbreInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * 短剧配音-角色
 */
class ItemDubbingTimbreVM(private val eventVm: ItemEventsViewModel<DubbingTimbreEvent>, data: DramasTimbreInfo?) : AppItemViewModel<DramasTimbreInfo>(data) {
    val selected by lazy { ObservableBoolean(data?.selectStatus == 1) }
    val playing by lazy { ObservableBoolean(false) }
    fun click() {
        eventVm.postEvent(DubbingTimbreEvent.Click(this))
    }
}