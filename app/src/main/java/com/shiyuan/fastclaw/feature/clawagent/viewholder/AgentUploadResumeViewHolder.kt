package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.manager.AppEventParam.XIAOXIA_TAB_NO_RESUME
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_NO_RESUME_START_QUESTIONING
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadResumeBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentItemEvent
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.router.AppNavigator

/**
 * 指引上传简历
 * @createtime 2026年05月27日 23:09
 * @author TooWhiteT
 */

class AgentUploadResumeViewHolder(
    private val binding: ItemMsgAgentUploadResumeBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo, isLastItem: Boolean) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }
        // 标题/说明：payload 非空时覆盖布局默认引导文案
        binding.tvUploadTitle.text = payload?.description?:binding.root.context.getString(R.string.agent_resume_no_conversation_tip)
        // 仅 WAITING_UPLOAD（或无状态）且整卡未禁用时可点上传；解析中等态置灰防重复提交
        val status = payload?.resumeUploadStatus
        val isWaiting = status.isNullOrBlank() || status == STATUS_WAITING_UPLOAD
        val enabled = payload?.interactionState?.disabled != true && isWaiting

        // 上传入口只在列表最后一条展示，后面出现新消息时隐藏整块操作区。
        binding.layoutAction.visibility = if (isLastItem) View.VISIBLE else View.GONE
        binding.btnUploadResume.isEnabled = enabled
        binding.btnUploadResume.alpha = if (enabled) 1f else 0.5f

        binding.btnUploadResume.setOnSingleClickListener {
            if (enabled) itemEvents.postEvent(ClawAgentItemEvent.UploadResume(item, false))
        }
        bindNotAction(item, payload)
    }

    private fun bindNotAction(item: ClawAgentMessageInfo, payload: UploadResumePayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_NO_RESUME_START_QUESTIONING }
        if (action == null) {
            binding.tvNotResume.gone()
            binding.tvNotResume.setOnClickListener(null)
            return
        }
        binding.tvNotResume.visible()
        binding.tvNotResume.isEnabled = action.disabled != true
        binding.tvNotResume.setOnSingleClickListener {
            //showToast(R.string.toast_resume_not_conversation)
            if (!AppNavigator.checkLoginIntercepted(false, loginSource = XIAOXIA_TAB_NO_RESUME)) {
                itemEvents.postEvent(ClawAgentItemEvent.DefaultAction(item, action, null))
            }
        }
    }

    companion object {
        private const val STATUS_WAITING_UPLOAD = "WAITING_UPLOAD"
    }

}
