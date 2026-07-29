package com.shiyuan.fastclaw.feature.interview

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.chad.library.adapter.base.BaseMultiItemAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemInterviewAiBinding
import com.shiyuan.fastclaw.databinding.ItemInterviewUserBinding
import com.shiyuan.fastclaw.feature.interview.bean.InterviewMessageInfo
import com.shiyuan.fastclaw.feature.interview.viewholder.InterviewAiViewHolder
import com.shiyuan.fastclaw.feature.interview.viewholder.InterviewUserViewHolder

/**
 * 面试房间消息列表适配器，负责区分面试官消息和用户消息并做局部刷新。
 */
class InterviewRoomMessageAdapter : BaseMultiItemAdapter<InterviewMessageInfo>() {

    init {
        // 面试官消息使用左侧 AI 气泡样式。
        register(TYPE_AGENT, { i, p -> InterviewAiViewHolder(ItemInterviewAiBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 用户作答消息使用右侧用户气泡样式。
        register(TYPE_USER, { i, p -> InterviewUserViewHolder(ItemInterviewUserBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 每条消息根据 senderType 动态选择对应的 itemType。
        onItemViewType { position, list -> list[position].itemType() }
    }

    override fun submitList(list: List<InterviewMessageInfo>?) {
        val newList = list ?: emptyList()
        // 使用 DiffUtil 保留同一条消息的局部更新效果，避免流式文本刷新时整列表重绘。
        val result = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items = newList
        result.dispatchUpdatesTo(this)
    }

    /**
     * 注册多类型 ViewHolder，统一封装 inflate 和 bind，减少 init 里的重复代码。
     */
    private inline fun <VH : RecyclerView.ViewHolder> register(itemType: Int, crossinline create: (inflater: LayoutInflater, parent: ViewGroup) -> VH, crossinline bind: (holder: VH, item: InterviewMessageInfo) -> Unit,
    ) {
        addItemType(itemType, object : OnMultiItemAdapterListener<InterviewMessageInfo, VH> {
            override fun onCreate(context: Context, parent: ViewGroup, viewType: Int): VH =
                create(LayoutInflater.from(context), parent)

            override fun onBind(holder: VH, position: Int, item: InterviewMessageInfo?) {
                item?.let { bind(holder, it) }
            }
        })
    }

    /**
     * 根据消息发送方映射列表类型，未知发送方默认按面试官消息展示。
     */
    private fun InterviewMessageInfo.itemType(): Int {
        return when (senderType) {
            InterviewViewModel.SENDER_USER -> TYPE_USER
            else -> TYPE_AGENT
        }
    }

    /**
     * 面试消息 Diff 回调，用消息 id 判断是否同一条，用对象整体判断内容是否变化。
     */
    private class MessageDiffCallback(
        private val oldList: List<InterviewMessageInfo>,
        private val newList: List<InterviewMessageInfo>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    companion object {
        /** 面试官消息类型。 */
        private const val TYPE_AGENT = 1
        /** 用户消息类型。 */
        private const val TYPE_USER = 2
    }
}
