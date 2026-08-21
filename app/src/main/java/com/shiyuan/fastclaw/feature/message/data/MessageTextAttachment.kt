package com.shiyuan.fastclaw.feature.message.data

import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.tulele.im.entity.MessageWrapper

// IM 聊天消息模型构建器，负责把云信消息转换为页面展示模型。
object MessageTextAttachment {

    /**
     * 将普通文本或暂不支持的消息转换成文本气泡模型。
     */
    fun buildChat(message: MessageWrapper, isSelf: Boolean, timeText: String, avatar: String?): TextChatItem {
        return TextChatItem(
            id = message.messageClientId,
            content = message.content?.takeIf { it.isNotBlank() } ?: App.instance.getString(R.string.message_preview_custom),
            avatar = avatar,
            isSelf = isSelf,
            time = message.time,
            timeText = timeText,
        )
    }
}
