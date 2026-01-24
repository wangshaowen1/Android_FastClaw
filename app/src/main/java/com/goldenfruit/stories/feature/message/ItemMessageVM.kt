package com.kgmedia.fastfilm.feature.message

import androidx.annotation.DrawableRes
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.response.MessageSummaryData
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * 消息item
 */
class ItemMessageVM(val eventVM: ItemEventsViewModel<Int>, private val type: Int, @DrawableRes icon: Int, val title: String, message: MessageSummaryData.Message) : AppItemViewModel<MessageSummaryData.Message>(message) {
    val icon by lazy { icon }
    fun onClick() {
        eventVM.postEvent(type)
    }

    fun createTime(): String {
        val time = this.data.get()?.createTime ?: 0
        if (time <= 0) return ""
        return DateUtil.parseTimeForPosting(time, false)
    }

}