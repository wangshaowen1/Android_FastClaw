package com.kgmedia.fastfilm.feature.message.systemnotice

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.SystemNoticeInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * 系统通知-图片
 */
class ItemSystemNoticeImageVM(
    private val eventVM: ItemEventsViewModel<ItemSystemNoticeEvent>,
    private val systemNoticeInfo: SystemNoticeInfo,
    imageUrl: String?
) : AppItemViewModel<String?>(imageUrl) {
    fun onClick() {
        eventVM.postEvent(ItemSystemNoticeEvent.ImageClick(systemNoticeInfo, data.get()))
    }
}