package com.shiyuan.fastclaw.feature.message

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.shiyuan.fastclaw.databinding.ItemMessageConversationBinding
import com.shiyuan.fastclaw.databinding.ItemMessageMarkedUnsuitableBinding
import com.shiyuan.fastclaw.feature.message.viewholder.ConversationViewHolder
import com.shiyuan.fastclaw.feature.message.viewholder.ConversationUnsuitableViewHolder

class MessageAdapter : BaseMultiItemAdapter<MessageListItem>() {

    var onConversationClick: (MessageConversationItem) -> Unit = {}
    var onMarkedUnsuitableClick: () -> Unit = {}

    init {
        register<ConversationViewHolder, MessageConversationItem>(TYPE_CONVERSATION, { inflater, parent ->
            ConversationViewHolder(ItemMessageConversationBinding.inflate(inflater, parent, false)).apply {
                onConversationClickListener = { onConversationClick(it) }
            }
        }) { holder, item -> holder.bind(item) }
        register<ConversationUnsuitableViewHolder, UnsuitableConversationItem>(TYPE_MARKED_UNSUITABLE, { inflater, parent ->
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
        const val TYPE_CONVERSATION = 1
        const val TYPE_MARKED_UNSUITABLE = 2
    }
}
