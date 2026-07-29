package com.shiyuan.fastclaw.feature.clawagent

import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.AgentQuickData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.databinding.ItemClawAgentQuickActionBinding

/** 小虾首页底部快捷入口适配器，入口标题和顺序来自服务端，图标使用本地映射。 */
class ClawQuickActionAdapter(private val onClick: (String?) -> Unit) : SingleBindingAdapter<ItemClawAgentQuickActionBinding, JobAgentModuleData>(
    layoutId = R.layout.item_claw_agent_quick_action,
    mData = emptyList(),
    onBind = { binding, _, item ->
        // 服务端只返回业务模块信息，图标仍按 reportType 使用本地资源。
        binding.ivIcon.setImageResource(localIconRes(item.reportType))
        binding.tvTitle.text = item.title.orEmpty()
        binding.root.setOnSingleClickListener {
            // 点击只回传 reportType，由页面统一决定具体跳转或发送动作。
            onClick(item.reportType)
        }
    },
) {

    companion object {
        /** reportType 到本地图标的映射表，用于过滤可展示入口和绑定图标。 */
        private val iconMap by lazy {
            defaultItems().associate { item -> item.reportType to item.iconRes }
        }

        /** 默认图标配置；不作为列表数据源，只作为服务端模块的本地图标映射。 */
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

        /** 仅展示已有本地图标映射的业务模块，避免未知 reportType 出现空图标入口。 */
        fun isSupported(reportType: String?): Boolean {
            return iconMap.containsKey(reportType)
        }

        /** 根据服务端 reportType 取本地图标资源。 */
        private fun localIconRes(reportType: String?): Int {
            return iconMap[reportType] ?: 0
        }
    }
}
