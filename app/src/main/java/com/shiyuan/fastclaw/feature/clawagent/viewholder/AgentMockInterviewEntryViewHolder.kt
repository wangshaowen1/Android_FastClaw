package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewEntryPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewEntryBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 模拟面试入口卡
 */
class AgentMockInterviewEntryViewHolder(private val binding: ItemMsgAgentMockInterviewEntryBinding) : RecyclerView.ViewHolder(binding.root) {

    var onActionClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewEntryPayloadBean::class.java) }.getOrNull()
        }
        val action = payload?.actions?.firstOrNull()
        binding.tvAction.text = binding.root.context.getString(R.string.str_agent_mock_interview_action_main)
        binding.tvCost.text = binding.root.context.getString(R.string.str_agent_mock_interview_action_cost)
        val enabled = action?.disabled != true && payload?.interactionState?.disabled != true
        binding.layoutAction.isEnabled = enabled
        binding.layoutAction.alpha = if (enabled) 1f else 0.5f
        binding.layoutAction.setOnSingleClickListener {
            if (action != null && enabled) {
                onActionClickListener(item, action, action.actionPayload?.questionQueueId)
            }
        }
    }
}
