package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewReportDoneBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentItemEvent
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 面试复盘报告 - 已生成
 */
class AgentMockInterviewReportDoneViewHolder(
    private val binding: ItemMsgAgentMockInterviewReportDoneBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewReportPayloadBean::class.java) }.getOrNull()
        }
        val time = payload?.interviewTime
        binding.tvReportTime.text = binding.root.context.getString(R.string.str_agent_mock_interview_time, time)
        binding.tvScore.text = payload?.totalScore?.toString()

        val action = payload?.actions?.firstOrNull()
        binding.tvAction.text = binding.root.context.getString(R.string.str_agent_mock_interview_report_view)
        //val enabled = action?.disabled != true
        //binding.tvAction.isEnabled = enabled
        //binding.tvAction.alpha = if (enabled) 1f else 0.5f
        binding.tvAction.setOnSingleClickListener {
            itemEvents.postEvent(ClawAgentItemEvent.InterviewDone(item, action, payload))
        }
    }
}
