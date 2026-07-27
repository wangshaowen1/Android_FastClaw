package com.shiyuan.fastclaw.feature.clawagent

import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.AgentQuickData
import com.shiyuan.fastclaw.core.network.apis.response.AgentQuickType
import com.shiyuan.fastclaw.databinding.ItemClawAgentQuickActionBinding

class ClawQuickActionAdapter(private val onClick: (AgentQuickType) -> Unit, ) : SingleBindingAdapter<ItemClawAgentQuickActionBinding, AgentQuickData>(
    layoutId = R.layout.item_claw_agent_quick_action,
    mData = emptyList(),
    onBind = { binding, _, item ->
        binding.ivIcon.setImageResource(item.iconRes)
        binding.tvTitle.setText(item.titleRes)
        binding.root.setOnSingleClickListener {
            onClick(item.type)
        }
    },
) {

    companion object {
        fun defaultItems(): List<AgentQuickData> {
            return listOf(
                AgentQuickData(
                    AgentQuickType.COMPANY_BACKGROUND,
                    R.drawable.icon_anent_back_check,
                    R.string.company_backcheck
                ),
                AgentQuickData(
                    AgentQuickType.RESUME_CHECK,
                    R.drawable.icon_agent_resume_check,
                    R.string.home_treasury_resume_check
                ),
                AgentQuickData(
                    AgentQuickType.SALARY_CHECK,
                    R.drawable.icon_agent_salary_check,
                    R.string.home_treasury_salary_check
                ),
                AgentQuickData(
                    AgentQuickType.MOCK_INTERVIEW,
                    R.drawable.icon_anent_interview,
                    R.string.mock_interview
                ),
                AgentQuickData(
                    AgentQuickType.PASS_PACKAGE,
                    R.drawable.icon_anent_interview_pass,
                    R.string.interview_pass_packet
                )
            )
        }
    }
}
