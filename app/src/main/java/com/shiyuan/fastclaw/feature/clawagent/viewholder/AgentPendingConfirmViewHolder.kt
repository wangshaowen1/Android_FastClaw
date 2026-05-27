package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.core.network.apis.response.HistoryResumeConfirmPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentPendingConfirmBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent历史简历确认卡（HISTORY_RESUME_CONFIRM）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentPendingConfirmViewHolder(private val binding: ItemMsgAgentPendingConfirmBinding) : RecyclerView.ViewHolder(binding.root) {

    /** 确认是本人简历点击（B 方案：仅暴露，提交后续统一接 sendAction(RESUME_CONFIRM_YES)） */
    var onConfirmClickListener: (item: ClawAgentMessageInfo) -> Unit = {}

    /** 否认（不是我的简历）点击（B 方案：仅暴露，提交后续统一接 sendAction(RESUME_CONFIRM_NO)） */
    var onRejectClickListener: (item: ClawAgentMessageInfo) -> Unit = {}

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, HistoryResumeConfirmPayloadBean::class.java) }.getOrNull()
        }
        // 文件名：payload 非空时覆盖布局默认文案
        payload?.resumeName?.takeIf { it.isNotBlank() }?.let { binding.tvFileName.text = it }
        payload?.resumeTime?.takeIf { it.isNotBlank() }?.let { binding.tvFileTime.text = "上传于$it" }
        // 整卡禁用时两个按钮置灰
        val disabled = payload?.interactionState?.disabled == true
        binding.btnConfirm.isEnabled = !disabled
        binding.btnReUpload.isEnabled = !disabled
        binding.btnConfirm.alpha = if (disabled) 0.5f else 1f
        binding.btnReUpload.alpha = if (disabled) 0.5f else 1f
        binding.btnConfirm.setOnSingleClickListener { if (!disabled) onConfirmClickListener(item) }
        binding.btnReUpload.setOnSingleClickListener { if (!disabled) onRejectClickListener(item) }
    }

}