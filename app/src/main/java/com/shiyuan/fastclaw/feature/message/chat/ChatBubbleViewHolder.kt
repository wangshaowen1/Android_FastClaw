package com.shiyuan.fastclaw.feature.message.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.databinding.ItemChatBubbleIncomingBinding
import com.shiyuan.fastclaw.databinding.ItemChatBubbleOutgoingBinding
import com.shiyuan.fastclaw.feature.message.data.BubbleChatItem
import com.shiyuan.fastclaw.feature.message.data.ChatMessageDirection

/**
 * 气泡消息公共 ViewHolder。
 *
 * 壳层统一处理方向、时间和头像；消息内容由 [ChatContentProvider] 创建并绑定。
 */
class ChatBubbleViewHolder<Item : BubbleChatItem> private constructor(
    private val shell: BubbleShell,
    private val contentView: ChatContentView<Item>,
    private val itemEvents: ItemEventsViewModel<MessageChatItemEvent>,
) : RecyclerView.ViewHolder(shell.root) {

    fun bind(item: Item) {
        check(item.direction == shell.direction) {
            "Bubble direction mismatch, expected=${shell.direction}, actual=${item.direction}"
        }
        shell.bind(item)
        if (shell.direction == ChatMessageDirection.INCOMING) {
            shell.avatarView.setOnSingleClickListener {
                itemEvents.postEvent(MessageChatItemEvent.HrAvatar(item))
            }
        }
        contentView.bind(item)
    }

    private interface BubbleShell {
        val root: View
        val avatarView: ImageView
        val contentFrame: ViewGroup
        val direction: ChatMessageDirection

        fun bind(item: BubbleChatItem)
    }

    private class IncomingBubbleShell(
        private val binding: ItemChatBubbleIncomingBinding,
    ) : BubbleShell {
        override val root: View = binding.root
        override val avatarView: ImageView = binding.ivAvatar
        override val contentFrame: ViewGroup = binding.contentFrame
        override val direction: ChatMessageDirection = ChatMessageDirection.INCOMING

        override fun bind(item: BubbleChatItem) {
            binding.item = item
            binding.executePendingBindings()
        }
    }

    private class OutgoingBubbleShell(
        private val binding: ItemChatBubbleOutgoingBinding,
    ) : BubbleShell {
        override val root: View = binding.root
        override val avatarView: ImageView = binding.ivAvatar
        override val contentFrame: ViewGroup = binding.contentFrame
        override val direction: ChatMessageDirection = ChatMessageDirection.OUTGOING

        override fun bind(item: BubbleChatItem) {
            binding.item = item
            binding.executePendingBindings()
        }
    }

    companion object {
        fun <Item : BubbleChatItem> create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            direction: ChatMessageDirection,
            contentProvider: ChatContentProvider<Item>,
            itemEvents: ItemEventsViewModel<MessageChatItemEvent>,
        ): ChatBubbleViewHolder<Item> {
            val shell = when (direction) {
                ChatMessageDirection.INCOMING -> IncomingBubbleShell(
                    ItemChatBubbleIncomingBinding.inflate(inflater, parent, false),
                )

                ChatMessageDirection.OUTGOING -> OutgoingBubbleShell(
                    ItemChatBubbleOutgoingBinding.inflate(inflater, parent, false),
                )
            }
            val contentView = contentProvider.create(inflater, shell.contentFrame)
            shell.contentFrame.addView(contentView.root)
            return ChatBubbleViewHolder(shell, contentView, itemEvents)
        }
    }
}
