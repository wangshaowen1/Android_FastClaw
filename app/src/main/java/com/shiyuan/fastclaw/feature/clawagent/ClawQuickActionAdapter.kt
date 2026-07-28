package com.shiyuan.fastclaw.feature.clawagent

import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.AgentQuickData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.databinding.ItemClawAgentQuickActionBinding

class ClawQuickActionAdapter(private val onClick: (String?) -> Unit) : SingleBindingAdapter<ItemClawAgentQuickActionBinding, JobAgentModuleData>(
    layoutId = R.layout.item_claw_agent_quick_action,
    mData = emptyList(),
    onBind = { binding, _, item ->
        binding.ivIcon.setImageResource(localIconRes(item.reportType))
        binding.tvTitle.text = item.title.orEmpty()
        binding.root.setOnSingleClickListener {
            onClick(item.reportType)
        }
    },
) {

    companion object {
        private val iconMap by lazy {
            defaultItems().associate { item -> item.reportType to item.iconRes }
        }

        fun defaultItems(): List<AgentQuickData> {
            return listOf(
                AgentQuickData(
                    R.drawable.icon_anent_back_check,
                    JobAgentModuleData.TYPE_COMPANY_RESEARCH
                ),
                AgentQuickData(
                    R.drawable.icon_agent_resume_check,
                    JobAgentModuleData.TYPE_RESUME_REJECTION_ANALYSIS
                ),
                AgentQuickData(
                    R.drawable.icon_agent_salary_check,
                    JobAgentModuleData.TYPE_SALARY_VALUATION
                ),
                AgentQuickData(
                    R.drawable.icon_anent_interview,
                    JobAgentModuleData.TYPE_MOCK_INTERVIEW
                ),
                AgentQuickData(
                    R.drawable.icon_anent_interview_pass,
                    JobAgentModuleData.TYPE_INTERVIEW_PASS_PACKAGE_REPORT
                )
            )
        }

        fun isSupported(reportType: String?): Boolean {
            return iconMap.containsKey(reportType)
        }

        private fun localIconRes(reportType: String?): Int {
            return iconMap[reportType] ?: 0
        }
    }
}
