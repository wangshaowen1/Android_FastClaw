package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_MOCK_INTERVIEW_NOT_NEEDED
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewEntryPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewEntryUpgradeBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 模拟面试理解升级入口卡
 */
class AgentMockInterviewEntryUpgradeViewHolder(private val binding: ItemMsgAgentMockInterviewEntryUpgradeBinding) : RecyclerView.ViewHolder(binding.root) {

    var onInterviewStartClickListener: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewEntryPayloadBean?) -> Unit = { _, _, _ -> }
    var onInterviewNotNeedClickListener: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewEntryPayloadBean?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewEntryPayloadBean::class.java) }.getOrNull()
        }
        val action = payload?.primaryAction ?: payload?.actions?.firstOrNull()
        val context = binding.root.context
        val enabled = payload?.buttonStatus != BUTTON_STATUS_DISABLED
        /*val enabled = action?.disabled != true &&
                payload?.interactionState?.disabled != true &&
                payload?.buttonStatus != BUTTON_STATUS_DISABLED &&
                !payload?.resumeId.isNullOrBlank()*/
        /*binding.tvAction.text = action?.text?.takeIf { it.isNotBlank() }
            ?: context.getString(
                R.string.str_agent_mock_interview_upgrade_action,
                payload?.chargeAmount ?: DEFAULT_CHARGE_AMOUNT,
                payload?.chargeUnit?.takeIf { it.isNotBlank() } ?: DEFAULT_CHARGE_UNIT
            )*/
        binding.tvCost.text = context.getString(R.string.str_agent_mock_interview_action_cost, payload?.chargeAmount?:0)
        binding.layoutAction.setBackgroundResource(if (enabled) R.drawable.bg_mock_interview_entry_action else R.drawable.bg_mock_interview_entry_action_disabled)
        binding.tvAction.setTextColor(ContextCompat.getColor(context, if (enabled) R.color.color_ffffff else R.color.color_55_3c3c43))
        binding.tvCost.setTextColor(ContextCompat.getColor(context, if (enabled) R.color.color_ffffff else R.color.color_55_3c3c43))
        binding.tvSkip.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.layoutAction.isEnabled = enabled
        binding.layoutAction.setOnSingleClickListener {
            onInterviewStartClickListener(item, action, payload)
        }
        bindSkipAction(item, payload)
    }

    private fun bindSkipAction(item: ClawAgentMessageInfo, payload: MockInterviewEntryPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_MOCK_INTERVIEW_NOT_NEEDED }
        if (action != null && payload.buttonStatus != BUTTON_STATUS_DISABLED) {
            binding.tvSkip.visible()
            binding.tvSkip.isEnabled = action.disabled != true
            binding.tvSkip.setOnSingleClickListener { onInterviewNotNeedClickListener(item, action, payload) }
        } else {
            binding.tvSkip.gone()
        }
    }

    companion object {
        private const val BUTTON_STATUS_DISABLED = "DISABLED"
        private const val DEFAULT_CHARGE_AMOUNT = 40
        private const val DEFAULT_CHARGE_UNIT = "虾条"
    }
}
