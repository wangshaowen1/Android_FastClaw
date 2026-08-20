package com.shiyuan.fastclaw.feature.message.chat.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.ItemChatResumeDeliveryBinding
import com.shiyuan.fastclaw.feature.message.data.ResumeDeliveryChatItem

/**
 * 简历投递消息 ViewHolder，负责渲染简历文件卡片和状态文案。
 */
class ResumeDeliveryViewHolder(private val binding: ItemChatResumeDeliveryBinding) : RecyclerView.ViewHolder(binding.root) {

    // 简历文件卡片点击回调。
    var onFileClickListener: (ResumeDeliveryChatItem) -> Unit = {}

    /**
     * 绑定简历投递卡片内容和文件点击事件。
     */
    fun bind(item: ResumeDeliveryChatItem) {
        binding.tvTime.text = item.timeText
        binding.tvTime.visibility = if (item.timeText.isBlank()) View.GONE else View.VISIBLE
        binding.tvTitle.text = item.title
        binding.tvFileName.text = item.fileName
        binding.tvStatus.text = item.statusText
        binding.layoutFile.setOnSingleClickListener { onFileClickListener(item) }
    }
}
