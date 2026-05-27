package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentResumeBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 指引上传简历
 * @createtime 2026年05月27日 23:09
 * @author TooWhiteT
 */

class AgentResumeViewHolder(private val binding: ItemMsgAgentResumeBinding) : RecyclerView.ViewHolder(binding.root) {

    var onUploadClickListener: (itemData: ClawAgentMessageInfo) -> Unit = {}

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }
        // 标题/说明：payload 非空时覆盖布局默认引导文案
        payload?.title?.takeIf { it.isNotBlank() }?.let { binding.tvUploadTitle.text = it }
        payload?.description?.takeIf { it.isNotBlank() }?.let { binding.tvUploadSubtitle.text = it }
        // 仅 WAITING_UPLOAD（或无状态）且整卡未禁用时可点上传；解析中等态置灰防重复提交
        val status = payload?.resumeUploadStatus
        val isWaiting = status.isNullOrBlank() || status == STATUS_WAITING_UPLOAD
        val enabled = payload?.interactionState?.disabled != true && isWaiting
        // 按钮文案：非等待态优先 statusText（如「简历解析中」），否则上传按钮 action.text / 默认
        val statusText = payload?.statusText
        val uploadActionText = payload?.actions?.firstOrNull()?.text
        binding.tvUploadBtn.text = when {
            !isWaiting && !statusText.isNullOrBlank() -> statusText
            !uploadActionText.isNullOrBlank() -> uploadActionText
            else -> binding.root.context.getString(R.string.home_upload_resume_btn)
        }
        binding.btnUploadResume.isEnabled = enabled
        binding.btnUploadResume.alpha = if (enabled) 1f else 0.5f
        binding.btnUploadResume.setOnSingleClickListener {
            if (enabled) onUploadClickListener(item)
        }
    }

    companion object {
        private const val STATUS_WAITING_UPLOAD = "WAITING_UPLOAD"
    }

}