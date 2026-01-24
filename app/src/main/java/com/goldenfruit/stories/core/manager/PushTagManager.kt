package com.kgmedia.fastfilm.core.manager

import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.core.User
import com.nhsd.basic.Session
import com.nhsd.basic.utils.Logger
import com.umeng.message.PushAgent
import java.util.Calendar

/**
 * 推送tag管理
 */
object PushTagManager {

    private val tagManager get() = PushAgent.getInstance(App.instance).tagManager

    @JvmStatic
    fun updateTags() {
        var tags = emptyList<String>()
        if (!User.get().isVisitor) {
            val creationTime = User.get().creationTime
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = creationTime
            val dayOfMonth = calendar[Calendar.DAY_OF_MONTH]
            val dayKey = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()
            val minute = calendar[Calendar.MINUTE]
            val minuteKey = if (minute < 10) "0$minute" else minute.toString()

            tags = listOfNotNull(
                "day_$dayKey",
                "minute_$minuteKey",
                Session.getInstance().channel,
                if (User.get().isFemale()) "female" else "male",
                "version_" + Session.getInstance().versionName,
                if (User.get().hasPaid()) "paid" else null
            )
        }
        tagManager?.getTags { b, existTags ->
            //未添加的标签
            val addTags = tags.filterNot { existTags?.contains(it) == true }
            //删除不存在的标签
            val deleteTags = existTags?.filterNot { tags.contains(it) }
            if (deleteTags.isNullOrEmpty()) {
                addTags(addTags)
            } else {
                tagManager?.deleteTags({ b, t ->
                    addTags(addTags)
                }, *deleteTags.toTypedArray())
            }
        }
    }

    private fun addTags(tags: List<String>) {
        if (tags.isEmpty()) return
        tagManager?.addTags({ b, t ->
            t?.jsonString?.let { Logger.i(it) }
        }, *tags.toTypedArray())
    }
}