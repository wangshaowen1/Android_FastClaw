package com.shiyuan.fastclaw.feature.message.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemMessageMarkedUnsuitableBinding
import com.shiyuan.fastclaw.feature.message.data.UnsuitableItem

/**
 * 已标记不合适入口 ViewHolder，负责渲染入口数量并转发点击事件。
 */
class ConversationUnsuitableViewHolder(private val binding: ItemMessageMarkedUnsuitableBinding, ) : RecyclerView.ViewHolder(binding.root) {

    var onMarkedUnsuitableClickListener: () -> Unit = {}

    /**
     * 绑定“已标记不合适的会话”入口行，当前只展示联系人数量与入口点击。
     */
    fun bind(item: UnsuitableItem) {
        binding.tvCount.text = binding.root.context.getString(R.string.message_marked_unsuitable_count, item.count)
        binding.root.setOnSingleClickListener { onMarkedUnsuitableClickListener() }
    }
}
