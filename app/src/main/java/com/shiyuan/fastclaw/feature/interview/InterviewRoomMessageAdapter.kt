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

class InterviewRoomMessageAdapter : BaseMultiItemAdapter<InterviewMessageInfo>() {

    init {
        register(TYPE_AGENT, { i, p -> AiViewHolder(ItemInterviewAiBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        register(TYPE_USER, { i, p -> UserViewHolder(ItemInterviewUserBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        onItemViewType { position, list -> list[position].itemType() }
    }

    private class AiViewHolder(private val binding: ItemInterviewAiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InterviewMessageInfo) {
            bindContent(item.content)
        }

        fun bindContent(content: String?) {
            binding.tvContent.text = content
        }
    }

    private class UserViewHolder(private val binding: ItemInterviewUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InterviewMessageInfo) {
            bindContent(item.content)
        }

        fun bindContent(content: String?) {
            binding.tvContent.text = content
        }
    }

    override fun submitList(list: List<InterviewMessageInfo>?) {
        val newList = list ?: emptyList()
        val result = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items = newList
        result.dispatchUpdatesTo(this)
    }

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

    private fun InterviewMessageInfo.itemType(): Int {
        return when (senderType) {
            InterviewViewModel.SENDER_USER -> TYPE_USER
            else -> TYPE_AGENT
        }
    }

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
        private const val TYPE_AGENT = 1
        private const val TYPE_USER = 2
    }
}
