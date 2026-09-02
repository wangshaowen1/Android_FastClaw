package com.shiyuan.fastclaw.feature.clawagent.message

import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.databinding.ItemMsgAgentActivityBinding

/** 聊天活动 - 测薪资卡片。 */
class AgentActivitySalaryViewHolder(
    private val binding: ItemMsgAgentActivityBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        binding.layoutContainer.setOnSingleClickListener {
            itemEvents.postEvent(ClawAgentItemEvent.ActivityClick(item))
        }
        binding.btnActivityAction.setOnSingleClickListener {
            itemEvents.postEvent(ClawAgentItemEvent.ActivityClick(item))
        }
    }
}
