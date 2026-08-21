package com.shiyuan.fastclaw.feature.message.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.shiyuan.fastclaw.databinding.ItemChatJobCardBinding
import com.shiyuan.fastclaw.databinding.ItemChatResumeDeliveryBinding
import com.shiyuan.fastclaw.databinding.ItemChatResumeRequestBinding
import com.shiyuan.fastclaw.databinding.ItemChatTextLeftBinding
import com.shiyuan.fastclaw.databinding.ItemChatTextRightBinding
import com.shiyuan.fastclaw.feature.message.chat.viewholder.JobChatViewHolder
import com.shiyuan.fastclaw.feature.message.chat.viewholder.ResumeDeliveryViewHolder
import com.shiyuan.fastclaw.feature.message.chat.viewholder.ResumeRequestViewHolder
import com.shiyuan.fastclaw.feature.message.chat.viewholder.TextChatLeftViewHolder
import com.shiyuan.fastclaw.feature.message.chat.viewholder.TextChatRightViewHolder
import com.shiyuan.fastclaw.feature.message.data.ChatMessageItem
import com.shiyuan.fastclaw.feature.message.data.JobChatItem
import com.shiyuan.fastclaw.feature.message.data.ResumeDeliveryChatItem
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestChatItem
import com.shiyuan.fastclaw.feature.message.data.TextChatItem

// IM 单聊消息适配器，负责职位、文本、简历请求和简历投递消息的多类型展示。
class MessageChatAdapter : BaseMultiItemAdapter<ChatMessageItem>() {

    // 简历请求同意点击回调，后续接入业务接口时由页面层处理。
    var onResumeRequestAcceptClick: (ResumeRequestChatItem) -> Unit = {}

    // 简历请求拒绝点击回调，后续接入业务接口时由页面层处理。
    var onResumeRequestRejectClick: (ResumeRequestChatItem) -> Unit = {}

    // 简历文件点击回调，后续接入预览或下载时由页面层处理。
    var onResumeFileClick: (ResumeDeliveryChatItem) -> Unit = {}

    init {
        // 职位卡片消息，展示首次沟通或切换职位的职位快照。
        register<JobChatViewHolder, JobChatItem>(TYPE_JOB_CARD, { inflater, parent ->
            JobChatViewHolder(ItemChatJobCardBinding.inflate(inflater, parent, false))
        }) { holder, item -> holder.bind(item) }

        // 左侧文本消息，展示对方发送的普通文本内容。
        register<TextChatLeftViewHolder, TextChatItem>(TYPE_TEXT_LEFT, { inflater, parent ->
            TextChatLeftViewHolder(ItemChatTextLeftBinding.inflate(inflater, parent, false))
        }) { holder, item -> holder.bind(item) }

        // 右侧文本消息，展示当前账号发送的普通文本内容。
        register<TextChatRightViewHolder, TextChatItem>(TYPE_TEXT_RIGHT, { inflater, parent ->
            TextChatRightViewHolder(ItemChatTextRightBinding.inflate(inflater, parent, false))
        }) { holder, item -> holder.bind(item) }

        // 索要简历消息，展示 HR 请求简历以及同意、拒绝操作状态。
        register<ResumeRequestViewHolder, ResumeRequestChatItem>(TYPE_RESUME_REQUEST, { inflater, parent ->
            ResumeRequestViewHolder(ItemChatResumeRequestBinding.inflate(inflater, parent, false)).apply {
                onAcceptClickListener = { onResumeRequestAcceptClick(it) }
                onRejectClickListener = { onResumeRequestRejectClick(it) }
            }
        }) { holder, item -> holder.bind(item) }

        // 主动发送简历消息，展示求职者发送的简历文件信息。
        register<ResumeDeliveryViewHolder, ResumeDeliveryChatItem>(TYPE_RESUME_DELIVERY, { inflater, parent ->
            ResumeDeliveryViewHolder(ItemChatResumeDeliveryBinding.inflate(inflater, parent, false)).apply {
                onFileClickListener = { onResumeFileClick(it) }
            }
        }) { holder, item -> holder.bind(item) }
        onItemViewType { position, list -> list[position].itemType }
    }

    /**
     * 提交聊天消息列表并通过 DiffUtil 做差量刷新。
     */
    override fun submitList(list: List<ChatMessageItem>?) {
        val newList = list.orEmpty()
        val result = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items = newList
        result.dispatchUpdatesTo(this)
    }

    /**
     * 注册单聊消息多类型 ViewHolder，统一封装 inflate 与类型安全 bind。
     */
    private inline fun <VH : RecyclerView.ViewHolder, reified Item : ChatMessageItem> register(
        itemType: Int,
        crossinline create: (inflater: LayoutInflater, parent: ViewGroup) -> VH,
        crossinline bind: (holder: VH, item: Item) -> Unit,
    ) {
        addItemType(itemType, object : OnMultiItemAdapterListener<ChatMessageItem, VH> {
            /**
             * 创建当前 itemType 对应的 ViewHolder。
             */
            override fun onCreate(context: Context, parent: ViewGroup, viewType: Int): VH =
                create(LayoutInflater.from(context), parent)

            /**
             * 将父类透传的通用 item 安全转换为目标类型后再绑定。
             */
            override fun onBind(holder: VH, position: Int, item: ChatMessageItem?) {
                (item as? Item)?.let { bind(holder, it) }
            }
        })
    }

    private class MessageDiffCallback(
        private val oldList: List<ChatMessageItem>,
        private val newList: List<ChatMessageItem>,
    ) : DiffUtil.Callback() {
        /**
         * 返回旧消息列表数量，供 DiffUtil 计算差量。
         */
        override fun getOldListSize(): Int = oldList.size

        /**
         * 返回新消息列表数量，供 DiffUtil 计算差量。
         */
        override fun getNewListSize(): Int = newList.size

        /**
         * 使用消息 id 和 itemType 判断是否为同一条消息。
         */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id &&
                oldList[oldItemPosition].itemType == newList[newItemPosition].itemType

        /**
         * data class 全等即可覆盖当前消息项的全部可见内容差异。
         */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    companion object {
        // 职位卡片消息类型。
        const val TYPE_JOB_CARD = 1
        // 左侧文本消息类型。
        const val TYPE_TEXT_LEFT = 2

        // 右侧文本消息类型。
        const val TYPE_TEXT_RIGHT = 3

        // 简历请求卡片消息类型。
        const val TYPE_RESUME_REQUEST = 4

        // 简历投递卡片消息类型。
        const val TYPE_RESUME_DELIVERY = 5
    }
}
