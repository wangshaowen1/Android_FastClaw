package com.shiyuan.fastclaw.feature.message.chat.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.ItemChatResumeRequestBinding
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestChatItem

/**
 * 简历请求消息 ViewHolder，负责渲染请求卡片和同意/拒绝操作入口。
 */
class ResumeRequestViewHolder(private val binding: ItemChatResumeRequestBinding) : RecyclerView.ViewHolder(binding.root) {

    // 同意简历请求点击回调。
    var onAcceptClickListener: (ResumeRequestChatItem) -> Unit = {}

    // 拒绝简历请求点击回调。
    var onRejectClickListener: (ResumeRequestChatItem) -> Unit = {}

    /**
     * 绑定简历请求卡片内容和点击事件。
     */
    fun bind(item: ResumeRequestChatItem) {
        binding.tvTime.text = item.timeText
        binding.tvTime.visibility = if (item.timeText.isBlank()) View.GONE else View.VISIBLE
        binding.tvTitle.text = item.title
        binding.tvStatus.text = item.statusText
        binding.tvAccept.setOnSingleClickListener { onAcceptClickListener(item) }
        binding.tvReject.setOnSingleClickListener { onRejectClickListener(item) }
    }
}
