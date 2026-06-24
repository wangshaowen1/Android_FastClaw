package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundReportFailedBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 公司背调报告 - 生成失败
 */
class AgentCompanyBackgroundReportFailedViewHolder(private val binding: ItemMsgAgentCompanyBackgroundReportFailedBinding) : RecyclerView.ViewHolder(binding.root) {

    var onCompanyClickListener: (item: ClawAgentMessageInfo, action: CardAction?, data: CompanyBackgroundReportPayloadBean?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CompanyBackgroundReportPayloadBean::class.java) }.getOrNull()
        }
        binding.tvFailedDesc.text = binding.root.context.getString(R.string.str_agent_company_background_report_failed_desc)
        val action = payload?.actions?.firstOrNull()
        binding.tvAction.text = binding.root.context.getString(R.string.str_agent_company_background_report_regenerate)
        val enabled = action?.disabled != true
        binding.tvAction.isEnabled = enabled
        binding.tvAction.alpha = if (enabled) 1f else 0.5f
        binding.tvAction.setOnSingleClickListener {
            onCompanyClickListener(item, action, payload)
        }
    }
}
