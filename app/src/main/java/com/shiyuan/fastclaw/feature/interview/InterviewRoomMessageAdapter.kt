package com.shiyuan.fastclaw.feature.interview

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.chad.library.adapter.base.BaseMultiItemAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemInterviewAiBinding
import com.shiyuan.fastclaw.databinding.ItemInterviewUserBinding

class InterviewRoomMessageAdapter : BaseMultiItemAdapter<InterviewRoomMessage>() {

    init {
        register(VIEW_TYPE_AI, { i, p -> AiViewHolder(ItemInterviewAiBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        register(VIEW_TYPE_USER, { i, p -> UserViewHolder(ItemInterviewUserBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        onItemViewType { position, list -> list[position].itemType() }
    }

    private class AiViewHolder(private val binding: ItemInterviewAiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InterviewRoomMessage) {
            binding.tvContent.text = item.content
        }
    }

    private class UserViewHolder(private val binding: ItemInterviewUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InterviewRoomMessage) {
            binding.tvContent.text = item.content
        }
    }

    override fun submitList(list: List<InterviewRoomMessage>?) {
        val newList = list ?: emptyList()
        val result = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items = newList
        result.dispatchUpdatesTo(this)
    }

    private inline fun <VH : RecyclerView.ViewHolder> register(itemType: Int, crossinline create: (inflater: LayoutInflater, parent: ViewGroup) -> VH, crossinline bind: (holder: VH, item: InterviewRoomMessage) -> Unit,
    ) {
        addItemType(itemType, object : OnMultiItemAdapterListener<InterviewRoomMessage, VH> {
            override fun onCreate(context: Context, parent: ViewGroup, viewType: Int): VH =
                create(LayoutInflater.from(context), parent)

            override fun onBind(holder: VH, position: Int, item: InterviewRoomMessage?) {
                item?.let { bind(holder, it) }
            }
        })
    }

    private fun InterviewRoomMessage.itemType(): Int {
        return when (role) {
            InterviewRoomMessage.Role.USER -> VIEW_TYPE_USER
            InterviewRoomMessage.Role.AI -> VIEW_TYPE_AI
        }
    }

    private class MessageDiffCallback(
        private val oldList: List<InterviewRoomMessage>,
        private val newList: List<InterviewRoomMessage>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    companion object {
        private const val VIEW_TYPE_AI = 1
        private const val VIEW_TYPE_USER = 2
    }
}
