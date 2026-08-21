package com.shiyuan.fastclaw.feature.message.chat.viewholder

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
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
        binding.tvFileName.text = item.fileName
        binding.tvStatus.text = item.status.deliveryStatusText()
        binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, item.statusColor()))
        Glide.with(binding.ivAvatar)
            .load(item.avatar)
            .placeholder(R.drawable.icon_default_avatar)
            .error(R.drawable.icon_default_avatar)
            .circleCrop()
            .into(binding.ivAvatar)
        binding.layoutFile.setOnSingleClickListener { onFileClickListener(item) }
    }

    /**
     * 根据简历投递状态返回状态展示文案。
     */
    private fun String?.deliveryStatusText(): String = when (this) {
        STATUS_ACCEPTED, STATUS_RECEIVED -> binding.root.context.getString(R.string.message_chat_resume_delivery_received)
        STATUS_REJECTED -> binding.root.context.getString(R.string.message_chat_resume_delivery_rejected)
        STATUS_PENDING -> binding.root.context.getString(R.string.message_chat_resume_delivery_sent)
        STATUS_SENT -> binding.root.context.getString(R.string.message_chat_resume_delivery_sent)
        else -> binding.root.context.getString(R.string.message_chat_resume_delivery_sent)
    }

    /**
     * 根据简历投递状态返回状态文案颜色。
     */
    private fun ResumeDeliveryChatItem.statusColor(): Int {
        return if (status == STATUS_PENDING || status == STATUS_SENT || status.isNullOrBlank()) {
            R.color.color_2f6bff
        } else {
            R.color.color_75_3c3c43
        }
    }

    companion object {
        // 主动发送简历待处理状态。
        private const val STATUS_PENDING = "pending"
        // 主动发送简历已接收状态。
        private const val STATUS_ACCEPTED = "accepted"
        // 主动发送简历已拒绝状态。
        private const val STATUS_REJECTED = "rejected"
        // 旧版本主动发送简历已发送状态。
        private const val STATUS_SENT = "sent"
        // 旧版本主动发送简历已接收状态。
        private const val STATUS_RECEIVED = "received"
    }
}
