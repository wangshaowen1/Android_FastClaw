package com.shiyuan.fastclaw.core.im

import com.google.gson.Gson
import com.tulele.im.custom.AttachmentParser
import com.tulele.im.custom.CustomAttachment

/**
 * 好友互动云信自定义消息，仅承载关系建立、申请拒绝和关系结束。
 *
 * 好友申请提交、屏蔽和取消屏蔽改由 [FriendRelationNotification] 处理。
 */
class FriendInteractionAttachment : CustomAttachment(MSG_TYPE) {

    var eventType: String? = null
    var actorContent: String? = null
    var targetContent: String? = null

    /** 根据当前消息方向返回动作方或目标方文案，缺失时使用另一侧文案兜底。 */
    fun displayContent(actorSide: Boolean): String {
        val primary = if (actorSide) actorContent else targetContent
        val fallback = if (actorSide) targetContent else actorContent
        return primary?.takeIf { it.isNotBlank() }
            ?: fallback?.takeIf { it.isNotBlank() }
            ?: ""
    }

    /** 拒绝和关系结束使用设计稿中的橙色警示样式。 */
    fun isWarningEvent(): Boolean = when (eventType) {
        EVENT_FRIEND_APPLY_REJECTED,
        EVENT_FRIEND_RELATION_ENDED -> true
        else -> false
    }

    /** 兼容过滤历史遗留的申请/屏蔽自定义消息，避免继续渲染成聊天气泡。 */
    fun isLegacyRelationNotification(): Boolean =
        FriendRelationNotification.isSupportedEvent(eventType)

    override fun getPushContent(): CharSequence = displayContent(actorSide = false)

    companion object {
        const val MSG_TYPE = 10004

        const val EVENT_FRIEND_RELATION_ESTABLISHED = "FRIEND_RELATION_ESTABLISHED"
        const val EVENT_FRIEND_APPLY_REJECTED = "FRIEND_APPLY_REJECTED"
        const val EVENT_FRIEND_RELATION_ENDED = "FRIEND_RELATION_ENDED"

        private val gson = Gson()

        @JvmField
        val PARSER = AttachmentParser { text ->
            runCatching {
                gson.fromJson(text, FriendInteractionAttachment::class.java)
            }.getOrNull()
        }
    }
}
