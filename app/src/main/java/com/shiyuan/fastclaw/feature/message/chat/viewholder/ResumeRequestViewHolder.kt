package com.shiyuan.fastclaw.feature.message.chat.viewholder

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemChatResumeRequestBinding
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestChatItem

/**
 * HR简历请求消息 ViewHolder，负责渲染请求卡片和同意/拒绝操作入口。
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
        binding.tvStatus.text = item.status.requestStatusText()
        binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, item.statusColor()))
        binding.tvStatus.visibility = if (item.isPending()) View.GONE else View.VISIBLE
        binding.layoutActions.visibility = if (item.isPending()) View.VISIBLE else View.GONE
        Glide.with(binding.ivAvatar)
            .load(item.avatar)
            .placeholder(R.drawable.icon_default_avatar)
            .error(R.drawable.icon_default_avatar)
            .circleCrop()
            .into(binding.ivAvatar)
        binding.tvAccept.setOnSingleClickListener { onAcceptClickListener(item) }
        binding.tvReject.setOnSingleClickListener { onRejectClickListener(item) }
    }

    /**
     * 判断简历请求是否处于待处理状态。
     */
    private fun ResumeRequestChatItem.isPending(): Boolean {
        return status == STATUS_PENDING || status.isNullOrBlank()
    }

    /**
     * 根据简历请求状态返回状态展示文案。
     */
    private fun String?.requestStatusText(): String = when (this) {
        STATUS_ACCEPTED, STATUS_SENT -> binding.root.context.getString(R.string.message_chat_resume_delivery_sent)
        STATUS_REJECTED -> binding.root.context.getString(R.string.message_chat_resume_request_rejected)
        else -> binding.root.context.getString(R.string.message_chat_resume_request_pending)
    }

    /**
     * 根据简历请求状态返回状态文案颜色。
     */
    private fun ResumeRequestChatItem.statusColor(): Int {
        return if (status == STATUS_PENDING || status == STATUS_SENT || status.isNullOrBlank()) {
            R.color.color_2f6bff
        } else {
            R.color.color_75_3c3c43
        }
    }

    companion object {
        // 简历请求待处理状态。
        private const val STATUS_PENDING = "pending"
        // 简历请求已同意状态。
        private const val STATUS_ACCEPTED = "accepted"
        // 简历请求已拒绝状态。
        private const val STATUS_REJECTED = "rejected"
        // 旧版本简历请求已发送状态。
        private const val STATUS_SENT = "sent"
    }
}
