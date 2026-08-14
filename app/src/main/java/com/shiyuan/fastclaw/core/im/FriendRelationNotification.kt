package com.shiyuan.fastclaw.core.im

import com.google.gson.Gson
import com.netease.nimlib.sdk.msg.model.CustomNotification
import com.nhsd.basic.bus.AppEvent

/**
 * 好友申请和双向屏蔽的云信自定义通知协议。
 *
 * 这类通知只驱动关系查询与页面状态，不写入聊天消息列表。
 */
object FriendRelationNotification {

    const val EVENT_FRIEND_APPLY_SUBMITTED = "FRIEND_APPLY_SUBMITTED"
    const val EVENT_FRIEND_INTERACTION_DISABLED = "FRIEND_INTERACTION_DISABLED"
    const val EVENT_USER_BLOCK_CANCELLED = "USER_BLOCK_CANCELLED"

    private val gson = Gson()
    private val supportedEventTypes = setOf(
        EVENT_FRIEND_APPLY_SUBMITTED,
        EVENT_FRIEND_INTERACTION_DISABLED,
        EVENT_USER_BLOCK_CANCELLED,
    )

    /** 云信通知 attach 的最小解析结构。 */
    private data class Payload(val eventType: String? = null)

    /**
     * 已解析的关系通知事件。
     *
     * @param fromAccount 发起事件的云信账号，用于页面过滤当前聊天对象。
     * @param eventType 服务端关系事件类型。
     * @param time 云信通知时间，单位毫秒。
     */
    data class Event(
        val fromAccount: String,
        val eventType: String,
        val time: Long,
    ) : AppEvent

    /** 解析受支持的关系通知，其他自定义通知继续交由各自业务处理。 */
    fun parse(notification: CustomNotification): Event? {
        val content = notification.content?.takeIf { it.isNotBlank() } ?: return null
        val payload = runCatching { gson.fromJson(content, Payload::class.java) }.getOrNull()
            ?: return null
        val eventType = payload.eventType?.takeIf(::isSupportedEvent) ?: return null
        val fromAccount = notification.fromAccount?.takeIf { it.isNotBlank() } ?: return null
        return Event(
            fromAccount = fromAccount,
            eventType = eventType,
            time = notification.time,
        )
    }

    /** 判断事件是否属于好友申请或双向屏蔽的自定义通知协议。 */
    fun isSupportedEvent(eventType: String?): Boolean = eventType in supportedEventTypes

    /** 屏蔽通知到达后先关闭输入能力，最终状态再由关系接口覆盖。 */
    fun shouldDisableInteractionImmediately(eventType: String): Boolean =
        eventType == EVENT_FRIEND_INTERACTION_DISABLED
}
