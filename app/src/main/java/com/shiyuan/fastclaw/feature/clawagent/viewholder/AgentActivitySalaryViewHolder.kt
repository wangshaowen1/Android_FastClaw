package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.ItemMsgAgentActivityBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** 聊天活动 - 测薪资卡片。 */
class AgentActivitySalaryViewHolder(private val binding: ItemMsgAgentActivityBinding) : RecyclerView.ViewHolder(binding.root) {

    var onActivityClickListener: (item: ClawAgentMessageInfo) -> Unit = {}

    fun bind(item: ClawAgentMessageInfo) {
        binding.layoutContainer.setOnSingleClickListener {
            onActivityClickListener(item)
        }
        binding.btnActivityAction.setOnSingleClickListener {
            onActivityClickListener(item)
        }
    }
}
