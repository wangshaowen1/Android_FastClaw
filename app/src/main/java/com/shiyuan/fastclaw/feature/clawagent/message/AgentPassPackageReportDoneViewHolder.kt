package com.shiyuan.fastclaw.feature.clawagent.message

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentPassPackageReportDoneBinding

/**
 * 面试通关包报告 - 已生成。
 */
class AgentPassPackageReportDoneViewHolder(
    private val binding: ItemMsgAgentPassPackageReportDoneBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, PassPackageReportPayloadBean::class.java) }.getOrNull()
        }
        val reportTime = payload?.reportTime
        binding.tvReportTime.text = binding.root.context.getString(R.string.str_agent_interview_pass_package_report_time, reportTime)
        binding.tvScore.text = payload?.passRate?.toString().orEmpty()

        val action = payload?.actions?.firstOrNull()
        binding.tvAction.text = binding.root.context.getString(R.string.str_agent_interview_pass_package_report_view)
        binding.tvAction.setOnSingleClickListener {
            itemEvents.postEvent(ClawAgentItemEvent.PassPackageDone(item, action, payload))
        }
    }
}
