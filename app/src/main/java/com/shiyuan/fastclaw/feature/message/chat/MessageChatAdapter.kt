package com.shiyuan.fastclaw.feature.message.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.databinding.ItemChatJobCardBinding
import com.shiyuan.fastclaw.feature.message.data.BubbleChatItem
import com.shiyuan.fastclaw.feature.message.data.ChatMessageContentType
import com.shiyuan.fastclaw.feature.message.data.ChatMessageDirection
import com.shiyuan.fastclaw.feature.message.data.ChatMessageItem
import com.shiyuan.fastclaw.feature.message.data.JobChatItem
import com.shiyuan.fastclaw.feature.message.data.ResumeDeliveryChatItem
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestChatItem
import com.shiyuan.fastclaw.feature.message.data.TextChatItem

// IM 单聊消息适配器；气泡内容由 Provider 扩展，业务卡片保持独立强类型实现。
class MessageChatAdapter(
    private val itemEvents: ItemEventsViewModel<MessageChatItemEvent>,
) : BaseMultiItemAdapter<ChatMessageItem>() {

    init {
        // 每个内容 Provider 自动组合两个方向壳层；新增内容类型时只需再注册一个 Provider。
        registerBubble<TextChatItem>(TextChatContentProvider())
        registerBubble<ResumeRequestChatItem>(ResumeRequestChatContentProvider(itemEvents))
        registerBubble<ResumeDeliveryChatItem>(ResumeDeliveryChatContentProvider(itemEvents))

        // 职位卡片是会话级业务内容，保持全宽独立布局。
        register<JobChatViewHolder, JobChatItem>(MessageChatViewType.TYPE_JOB_CARD, { inflater, parent ->
            JobChatViewHolder(
                binding = ItemChatJobCardBinding.inflate(inflater, parent, false),
                itemEvents = itemEvents,
            )
        }) { holder, item -> holder.bind(item) }

        onItemViewType { position, list ->
            MessageChatViewType.of(list[position])
        }
    }

    /** 一个内容 Provider 自动注册 Incoming/Outgoing 两个公共壳层。 */
    private inline fun <reified Item : BubbleChatItem> registerBubble(
        provider: ChatContentProvider<Item>,
    ) {
        ChatMessageDirection.values().forEach { direction ->
            register<ChatBubbleViewHolder<Item>, Item>(
                itemType = MessageChatViewType.bubble(provider.contentType, direction),
                create = { inflater, parent ->
                    ChatBubbleViewHolder.create(
                        inflater = inflater,
                        parent = parent,
                        direction = direction,
                        contentProvider = provider,
                        itemEvents = itemEvents,
                    )
                },
                bind = { holder, item ->
                    check(item.contentType == provider.contentType) {
                        "Bubble content mismatch, expected=${provider.contentType}, actual=${item.contentType}"
                    }
                    holder.bind(item)
                },
            )
        }
    }

    /** 提交聊天消息列表并通过 DiffUtil 做差量刷新。 */
    override fun submitList(list: List<ChatMessageItem>?) {
        val newList = list.orEmpty()
        val result = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items = newList
        result.dispatchUpdatesTo(this)
    }

    /** 注册单聊消息多类型 ViewHolder，统一封装 inflate 与类型安全 bind。 */
    private inline fun <VH : RecyclerView.ViewHolder, reified Item : ChatMessageItem> register(
        itemType: Int,
        crossinline create: (inflater: LayoutInflater, parent: ViewGroup) -> VH,
        crossinline bind: (holder: VH, item: Item) -> Unit,
    ) {
        addItemType(itemType, object : OnMultiItemAdapterListener<ChatMessageItem, VH> {
            override fun onCreate(context: Context, parent: ViewGroup, viewType: Int): VH =
                create(LayoutInflater.from(context), parent)

            override fun onBind(holder: VH, position: Int, item: ChatMessageItem?) {
                check(item is Item) {
                    "Message viewType/item mismatch, expected=${Item::class.java.simpleName}, " +
                        "actual=${item?.javaClass?.simpleName}"
                }
                bind(holder, item)
            }
        })
    }

    private class MessageDiffCallback(
        private val oldList: List<ChatMessageItem>,
        private val newList: List<ChatMessageItem>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        /** 消息 id 相同且最终视图类型一致，才视为同一列表项。 */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id &&
                MessageChatViewType.of(oldList[oldItemPosition]) == MessageChatViewType.of(newList[newItemPosition])

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    /** Adapter 内部 viewType 编码，避免数据模型依赖 RecyclerView 实现。 */
    private object MessageChatViewType {
        private const val OUTGOING_MASK = 1 shl 16
        private const val STANDALONE_BASE = 1 shl 20
        const val TYPE_JOB_CARD = STANDALONE_BASE + 1

        fun bubble(contentType: ChatMessageContentType, direction: ChatMessageDirection): Int {
            check(contentType.stableId in 1 until OUTGOING_MASK) {
                "Chat content stableId must be in 1 until $OUTGOING_MASK"
            }
            return contentType.stableId or
                (if (direction == ChatMessageDirection.OUTGOING) OUTGOING_MASK else 0)
        }

        fun of(item: ChatMessageItem): Int = when (item) {
            is BubbleChatItem -> bubble(item.contentType, item.direction)
            is JobChatItem -> TYPE_JOB_CARD
        }
    }
}
