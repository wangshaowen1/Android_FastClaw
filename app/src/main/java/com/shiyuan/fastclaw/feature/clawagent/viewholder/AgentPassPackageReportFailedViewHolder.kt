package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentPassPackageReportFailedBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 面试通关包报告 - 生成失败。
 */
class AgentPassPackageReportFailedViewHolder(private val binding: ItemMsgAgentPassPackageReportFailedBinding) : RecyclerView.ViewHolder(binding.root) {

    var onPassPackageRetryClickListener: (item: ClawAgentMessageInfo, action: CardAction?, data: PassPackageReportPayloadBean?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, PassPackageReportPayloadBean::class.java) }.getOrNull()
        }
        binding.tvFailedDesc.text = binding.root.context.getString(R.string.str_agent_interview_pass_package_report_failed_desc)
        val action = payload?.actions?.firstOrNull()
        binding.tvAction.text = binding.root.context.getString(R.string.str_agent_interview_pass_package_report_regenerate)
        binding.tvAction.setOnSingleClickListener {
            onPassPackageRetryClickListener(item, action, payload)
        }
    }
}
