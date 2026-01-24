package com.kgmedia.fastfilm.feature.message.systemnotice

import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.network.apis.entity.SystemNoticeInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * 系统通知
 */
class ItemSystemNoticeVM(private val eventVM: ItemEventsViewModel<ItemSystemNoticeEvent>, info: SystemNoticeInfo?) : AppItemViewModel<SystemNoticeInfo>(info) {
    val imageAdapter by lazy {
        info?.imageList
            ?.mapIndexed { index, imageUrl -> ItemSystemNoticeImageVM(eventVM, info, imageUrl) }
            ?.let { QuickAdapter(R.layout.item_system_notice_image, it) }
    }

    fun onClick() {
        eventVM.postEvent(ItemSystemNoticeEvent.Click(data.get()))
    }
}
