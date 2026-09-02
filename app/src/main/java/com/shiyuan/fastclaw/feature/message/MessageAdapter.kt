package com.shiyuan.fastclaw.feature.message

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.shiyuan.fastclaw.databinding.ItemMessageConversationBinding
import com.shiyuan.fastclaw.databinding.ItemMessageMarkedUnsuitableBinding
import com.shiyuan.fastclaw.feature.message.data.ConversationItem
import com.shiyuan.fastclaw.feature.message.data.MessageListItem
import com.shiyuan.fastclaw.feature.message.data.UnsuitableEntry

// 消息会话列表适配器，负责普通会话和不合适会话入口的多类型展示。
class MessageAdapter : BaseMultiItemAdapter<MessageListItem>() {

    // 普通会话列表项点击回调，由页面层决定后续跳转或业务处理。
    var onConversationClick: (ConversationItem) -> Unit = {}

    // 已标记不合适会话入口点击回调，由页面层打开对应落地页。
    var onMarkedUnsuitableClick: () -> Unit = {}

    init {
        register<ConversationViewHolder, ConversationItem>(TYPE_CONVERSATION, { inflater, parent ->
            ConversationViewHolder(ItemMessageConversationBinding.inflate(inflater, parent, false)).apply {
                onConversationClickListener = { onConversationClick(it) }
            }
        }) { holder, item -> holder.bind(item) }
        register<ConversationViewHolder, ConversationItem>(TYPE_UNSUITABLE, { inflater, parent ->
            ConversationViewHolder(ItemMessageConversationBinding.inflate(inflater, parent, false)).apply {
                onConversationClickListener = { onConversationClick(it) }
            }
        }) { holder, item -> holder.bind(item) }
        register<ConversationUnsuitableViewHolder, UnsuitableEntry>(TYPE_UNSUITABLE_ENTRY, { inflater, parent ->
            ConversationUnsuitableViewHolder(ItemMessageMarkedUnsuitableBinding.inflate(inflater, parent, false)).apply {
                onMarkedUnsuitableClickListener = { onMarkedUnsuitableClick() }
            }
        }) { holder, item -> holder.bind(item) }
        onItemViewType { position, list -> list[position].itemType }
    }

    /**
     * 提交新列表并通过 DiffUtil 做差量刷新，保留 item 更新动画与滚动位置。
     */
    override fun submitList(list: List<MessageListItem>?) {
        val newList = list.orEmpty()
        val result = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items = newList
        result.dispatchUpdatesTo(this)
    }

    /**
     * 注册消息列表的多类型 ViewHolder，统一封装 inflate 与类型安全 bind。
     */
    private inline fun <VH : RecyclerView.ViewHolder, reified Item : MessageListItem> register(
        itemType: Int,
        crossinline create: (inflater: LayoutInflater, parent: ViewGroup) -> VH,
        crossinline bind: (holder: VH, item: Item) -> Unit,
    ) {
        addItemType(itemType, object : OnMultiItemAdapterListener<MessageListItem, VH> {
            /**
             * 创建当前 itemType 对应的 ViewHolder。
             */
            override fun onCreate(context: Context, parent: ViewGroup, viewType: Int): VH =
                create(LayoutInflater.from(context), parent)

            /**
             * 将父类透传的通用 item 安全转换为目标类型后再绑定。
             */
            override fun onBind(holder: VH, position: Int, item: MessageListItem?) {
                (item as? Item)?.let { bind(holder, it) }
            }
        })
    }

    private class MessageDiffCallback(
        private val oldList: List<MessageListItem>,
        private val newList: List<MessageListItem>,
    ) : DiffUtil.Callback() {
        /**
         * 返回旧列表数量，供 DiffUtil 计算差量。
         */
        override fun getOldListSize(): Int = oldList.size

        /**
         * 返回新列表数量，供 DiffUtil 计算差量。
         */
        override fun getNewListSize(): Int = newList.size

        /**
         * 使用业务 id 和 itemType 判断是否为同一条列表项。
         */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id &&
                oldList[oldItemPosition].itemType == newList[newItemPosition].itemType

        /**
         * data class 全等即可覆盖当前列表项的全部可见内容差异。
         */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    companion object {
        // 普通 IM 会话列表项类型。
        const val TYPE_CONVERSATION = 1

        // 已标记不合适会话入口。
        const val TYPE_UNSUITABLE_ENTRY = 2

        // 已标记不合适会话表项类型。
        const val TYPE_UNSUITABLE = 3
    }
}
