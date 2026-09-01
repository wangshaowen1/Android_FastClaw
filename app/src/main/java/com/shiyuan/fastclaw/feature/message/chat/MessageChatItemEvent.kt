package com.shiyuan.fastclaw.feature.message.chat

import com.shiyuan.fastclaw.feature.message.data.ChatMessageItem
import com.shiyuan.fastclaw.feature.message.data.JobChatItem
import com.shiyuan.fastclaw.feature.message.data.ResumeDeliveryChatItem
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestChatItem

/**
 * IM 单聊消息列表 item 事件。
 *
 * Adapter/ViewHolder 只负责转发，页面统一消费；messageType 透传被点击消息的列表类型。
 */
sealed class MessageChatItemEvent {

    /** 被点击的原始消息项。 */
    abstract val item: ChatMessageItem

    /** 被点击消息的列表类型，对应 MessageChatAdapter 中注册的 TYPE 常量。 */
    val messageType: Int
        get() = item.itemType

    /** 点击职位卡片。 */
    data class JobCard(
        override val item: JobChatItem,
    ) : MessageChatItemEvent()

    /** 点击 HR 头像。 */
    data class HrAvatar(
        override val item: ChatMessageItem,
    ) : MessageChatItemEvent()

    /** 同意 HR 的简历请求。 */
    data class ResumeRequestAccept(
        override val item: ResumeRequestChatItem,
    ) : MessageChatItemEvent()

    /** 拒绝 HR 的简历请求。 */
    data class ResumeRequestReject(
        override val item: ResumeRequestChatItem,
    ) : MessageChatItemEvent()

    /** 点击已投递的简历文件。 */
    data class ResumeFile(
        override val item: ResumeDeliveryChatItem,
    ) : MessageChatItemEvent()
}
