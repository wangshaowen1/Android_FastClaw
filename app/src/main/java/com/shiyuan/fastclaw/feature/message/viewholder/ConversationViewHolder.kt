package com.shiyuan.fastclaw.feature.message.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.formatMessageNum
import com.shiyuan.fastclaw.databinding.ItemMessageConversationBinding
import com.shiyuan.fastclaw.feature.message.data.ConversationItem
import com.shiyuan.fastclaw.feature.message.MessageTimeFormatter

/**
 * 普通 P2P 会话列表项 ViewHolder，负责渲染联系人信息与末条消息摘要。
 */
class ConversationViewHolder(private val binding: ItemMessageConversationBinding, ) : RecyclerView.ViewHolder(binding.root) {

    var onConversationClickListener: (ConversationItem) -> Unit = {}

    /**
     * 绑定普通 P2P 会话行，包含头像、未读数、草稿标记与末条消息摘要。
     */
    fun bind(item: ConversationItem) {
        binding.tvName.text = item.targetName
        binding.tvDesc.text = item.targetDesc.orEmpty()
        binding.tvDesc.visibility = if (item.targetDesc.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvLastMessage.text = item.lastMessage
        binding.tvTime.text = MessageTimeFormatter.format(item.lastMessageTime)
        binding.sbUnreadCount.text = item.unreadCount.formatMessageNum()
        binding.sbUnreadCount.visibility = if (item.unreadCount > 0) View.VISIBLE else View.GONE
        binding.tvDraft.visibility = if (item.isDraft) View.VISIBLE else View.GONE
        Glide.with(binding.ivAvatar)
            .load(item.targetAvatar)
            .placeholder(R.drawable.ic_default_avatar_big)
            .error(R.drawable.ic_default_avatar_big)
            .circleCrop()
            .into(binding.ivAvatar)
        binding.root.setOnSingleClickListener { onConversationClickListener(item) }
    }
}
