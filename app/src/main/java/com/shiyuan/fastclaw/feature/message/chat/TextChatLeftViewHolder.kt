package com.shiyuan.fastclaw.feature.message.chat

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.blankj.utilcode.util.ClipboardUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemChatTextLeftBinding
import com.shiyuan.fastclaw.feature.message.data.TextChatItem

/**
 * 左侧文本消息 ViewHolder，负责渲染对方头像和文本气泡。
 */
class TextChatLeftViewHolder(private val binding: ItemChatTextLeftBinding) : RecyclerView.ViewHolder(binding.root) {

    // 对方头像点击回调。
    var onAvatarClickListener: (TextChatItem) -> Unit = {}

    /**
     * 绑定左侧文本消息内容。
     */
    fun bind(item: TextChatItem) {
        binding.tvTime.text = item.timeText
        binding.tvTime.visibility = if (item.timeText.isBlank()) View.GONE else View.VISIBLE
        binding.tvContent.text = item.content
        Glide.with(binding.ivAvatar)
            .load(item.avatar)
            .placeholder(R.drawable.icon_default_avatar)
            .error(R.drawable.icon_default_avatar)
            .circleCrop()
            .into(binding.ivAvatar)
        binding.ivAvatar.setOnSingleClickListener { onAvatarClickListener(item) }
        binding.tvContent.setOnLongClickListener {
            binding.root.context.getString(R.string.toast_copied).showToast()
            ClipboardUtils.copyText(item.content)
            true
        }
    }
}
