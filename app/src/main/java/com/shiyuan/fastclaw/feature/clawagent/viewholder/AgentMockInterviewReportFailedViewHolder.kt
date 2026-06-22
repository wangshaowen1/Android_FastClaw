package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewReportFailedBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 面试复盘报告 - 生成失败
 */
class AgentMockInterviewReportFailedViewHolder(private val binding: ItemMsgAgentMockInterviewReportFailedBinding) : RecyclerView.ViewHolder(binding.root) {

    var onActionClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewReportPayloadBean::class.java) }.getOrNull()
        }
        binding.tvFailedDesc.text = binding.root.context.getString(R.string.str_agent_mock_interview_report_failed_desc)
        val action = payload?.actions?.firstOrNull()
        binding.tvAction.text = binding.root.context.getString(R.string.str_agent_mock_interview_report_regenerate)
        val enabled = action?.disabled != true
        binding.tvAction.isEnabled = enabled
        binding.tvAction.alpha = if (enabled) 1f else 0.5f
        binding.tvAction.setOnSingleClickListener {
            if (action != null && enabled) {
                onActionClickListener(item, action, action.actionPayload?.questionQueueId)
            }
        }
    }
}
