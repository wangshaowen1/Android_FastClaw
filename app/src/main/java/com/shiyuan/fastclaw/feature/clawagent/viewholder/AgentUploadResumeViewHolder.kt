package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_NO_RESUME_START_QUESTIONING
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadResumeBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.router.AppNavigator

/**
 * 指引上传简历
 * @createtime 2026年05月27日 23:09
 * @author TooWhiteT
 */

class AgentUploadResumeViewHolder(private val binding: ItemMsgAgentUploadResumeBinding) : RecyclerView.ViewHolder(binding.root) {

    var onUploadClickListener: (itemData: ClawAgentMessageInfo) -> Unit = {}

    /** 我没有简历 */
    var onDefaultClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_NO_RESUME_START_QUESTIONING }
        // 标题/说明：payload 非空时覆盖布局默认引导文案
        binding.tvUploadTitle.text = payload?.description?:binding.root.context.getString(R.string.agent_resume_no_conversation_tip)
        // 仅 WAITING_UPLOAD（或无状态）且整卡未禁用时可点上传；解析中等态置灰防重复提交
        val status = payload?.resumeUploadStatus
        val isWaiting = status.isNullOrBlank() || status == STATUS_WAITING_UPLOAD
        val enabled = payload?.interactionState?.disabled != true && isWaiting

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
