package com.shiyuan.fastclaw.feature.message.data

import com.tulele.im.entity.MessageDirection
import com.tulele.im.entity.MessageType
import com.tulele.im.entity.MessageWrapper

/**
 * 云信 SDK 消息到页面不可变消息模型的统一边界。
 *
 * SDK 版本相关的类型判断停留在这里，Adapter 和 Provider 只消费 [ChatMessageItem]。
 */
class MessageChatItemFactory(
    private val isLocalMessage: (messageClientId: String) -> Boolean,
    private val avatarProvider: (isSelf: Boolean) -> String?,
) {

    fun create(message: MessageWrapper, timeText: String): ChatMessageItem {
        val isSelf = message.direct == MessageDirection.Out || isLocalMessage(message.messageClientId)
        return when {
            message.messageType == MessageType.custom && message.subtype == ResumeRequestAttachment.MSG_TYPE -> {
                ResumeRequestAttachment.buildChat(message, timeText, avatarProvider(isSelf))
            }

            message.messageType == MessageType.custom && message.subtype == ResumeDeliveryAttachment.MSG_TYPE -> {
                ResumeDeliveryAttachment.buildChat(message, isSelf, timeText)
            }

            message.messageType == MessageType.custom && message.subtype == MessageJobAttachment.MSG_TYPE -> {
                MessageJobAttachment.buildChat(message, timeText)
            }

            else -> MessageTextAttachment.buildChat(message, isSelf, timeText, avatarProvider(isSelf))
        }
    }
}
