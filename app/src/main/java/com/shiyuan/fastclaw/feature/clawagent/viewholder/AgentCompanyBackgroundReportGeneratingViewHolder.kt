package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundReportGeneratingBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 公司背调报告 - 生成中
 */
class AgentCompanyBackgroundReportGeneratingViewHolder(private val binding: ItemMsgAgentCompanyBackgroundReportGeneratingBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        var data = item.metaData
    }
}
