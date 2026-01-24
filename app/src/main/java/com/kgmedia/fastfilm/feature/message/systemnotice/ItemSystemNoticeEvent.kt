package com.kgmedia.fastfilm.feature.message.systemnotice

import com.kgmedia.fastfilm.core.network.apis.entity.SystemNoticeInfo

/**
 * 系统通知item点击事件
 */
interface ItemSystemNoticeEvent {
    class Click(val systemNoticeInfo: SystemNoticeInfo?) : ItemSystemNoticeEvent
    class ImageClick(val systemNoticeInfo: SystemNoticeInfo?, val imageUrl: String?) : ItemSystemNoticeEvent
}