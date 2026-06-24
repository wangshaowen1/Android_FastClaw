package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundReportDoneBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 公司背调报告 - 已生成
 */
class AgentCompanyBackgroundReportDoneViewHolder(private val binding: ItemMsgAgentCompanyBackgroundReportDoneBinding) : RecyclerView.ViewHolder(binding.root) {

    var onCompanyClickListener: (item: ClawAgentMessageInfo, action: CardAction, data: CompanyBackgroundReportPayloadBean?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CompanyBackgroundReportPayloadBean::class.java) }.getOrNull()
        }
        binding.tvCompanyName.text = payload?.companyName
        binding.tvReportTime.text = binding.root.context.getString(R.string.str_agent_company_background_report_time, payload?.generateTime)

        val action = payload?.actions?.firstOrNull()
        binding.tvAction.text = binding.root.context.getString(R.string.str_agent_company_background_report_view)
        val enabled = action?.disabled != true
        binding.tvAction.isEnabled = enabled
        binding.tvAction.alpha = if (enabled) 1f else 0.5f
        binding.tvAction.setOnSingleClickListener {
            if (action != null && enabled) {
                onCompanyClickListener(item, action, payload)
            }
        }
    }
}
