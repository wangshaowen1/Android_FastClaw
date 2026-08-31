package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.HistoryResumeConfirmPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadConfirmBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentItemEvent
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent历史简历确认卡（HISTORY_RESUME_CONFIRM）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentUploadConfirmViewHolder(
    private val binding: ItemMsgAgentUploadConfirmBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo, isLastItem: Boolean) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, HistoryResumeConfirmPayloadBean::class.java) }.getOrNull()
        }
        binding.tvDescTitle.text = payload?.description?:binding.root.context.getString(R.string.resume_confirm_card_title)
        payload?.resumeName?.takeIf { it.isNotBlank() }?.let { binding.tvFileName.text = it }
        payload?.resumeTime?.takeIf { it.isNotBlank() }?.let { binding.tvFileTime.text = binding.root.context.getString(R.string.resume_confirm_upload_time, it) }
        // 整卡禁用时两个按钮置灰
        val disabled = payload?.interactionState?.disabled == true
        // 确认按钮只在列表最后一条展示，后面出现新消息时隐藏整块操作区。
        binding.layoutAction.visibility = if (isLastItem) View.VISIBLE else View.GONE
        binding.btnConfirm.isEnabled = !disabled
        binding.btnReUpload.isEnabled = !disabled
        binding.btnConfirm.alpha = if (disabled) 0.5f else 1f
        binding.btnReUpload.alpha = if (disabled) 0.5f else 1f
        binding.btnConfirm.setOnSingleClickListener {
            if (!disabled) itemEvents.postEvent(ClawAgentItemEvent.ResumeConfirm(item, true))
        }
        binding.btnReUpload.setOnSingleClickListener {
            if (!disabled) itemEvents.postEvent(ClawAgentItemEvent.ResumeConfirm(item, false))
        }
    }
}
