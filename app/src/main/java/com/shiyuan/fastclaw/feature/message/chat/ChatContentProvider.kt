package com.shiyuan.fastclaw.feature.message.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.shiyuan.fastclaw.feature.message.data.BubbleChatItem
import com.shiyuan.fastclaw.feature.message.data.ChatMessageContentType

/**
 * 气泡内容 Provider。
 *
 * Provider 只负责一种消息内容及其事件转发，不负责消息方向、头像或时间。
 */
interface ChatContentProvider<Item : BubbleChatItem> {
    val contentType: ChatMessageContentType

    fun create(inflater: LayoutInflater, parent: ViewGroup): ChatContentView<Item>
}

/** Provider 创建的内容视图，绑定入口统一收敛为不可变的 [BubbleChatItem]。 */
interface ChatContentView<Item : BubbleChatItem> {
    val root: View

    fun bind(item: Item)
}
