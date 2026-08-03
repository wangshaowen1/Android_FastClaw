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

    /** 点击“开始模拟面试”时回调给 Adapter/Fragment，由外层统一提交卡片动作。 */
    var onInterviewStartClickListener: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewEntryPayloadBean?) -> Unit = { _, _, _ -> }

    /** 点击“暂不需要”时回调给 Adapter/Fragment，由外层统一提交跳过动作。 */
    var onInterviewNotNeedClickListener: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewEntryPayloadBean?) -> Unit = { _, _, _ -> }

    /** 绑定模拟面试理解升级入口卡内容和按钮状态。 */
    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewEntryPayloadBean::class.java) }.getOrNull()
        }
        // 主按钮优先使用 primaryAction，兼容旧数据从 actions 第一项取。
        val action = payload?.primaryAction ?: payload?.actions?.firstOrNull()
        val context = binding.root.context
        // 服务端置为 DISABLED 时，主按钮置灰且隐藏跳过入口，避免继续提交动作。
        val enabled = payload?.buttonStatus != BUTTON_STATUS_DISABLED
        binding.tvCost.text = context.getString(R.string.str_agent_mock_interview_action_cost, payload?.chargeAmount?:0)
        binding.btnAction.setBackgroundResource(if (enabled) R.drawable.bg_mock_interview_entry_action else R.drawable.bg_mock_interview_entry_action_disabled)
        binding.tvAction.setTextColor(ContextCompat.getColor(context, if (enabled) R.color.color_ffffff else R.color.color_55_3c3c43))
        binding.tvCost.setTextColor(ContextCompat.getColor(context, if (enabled) R.color.color_ffffff else R.color.color_55_3c3c43))
        binding.tvSkip.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.btnAction.isEnabled = enabled
        binding.btnAction.setOnSingleClickListener {
            onInterviewStartClickListener(item, action, payload)
        }
        bindSkipAction(item, payload)
    }

    /** 单独绑定“暂不需要”动作，只有服务端下发对应 action 且按钮可用时才展示。 */
    private fun bindSkipAction(item: ClawAgentMessageInfo, payload: MockInterviewEntryPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_MOCK_INTERVIEW_NOT_NEEDED }
        if (action != null && payload.buttonStatus != BUTTON_STATUS_DISABLED) {
            binding.tvSkip.visible()
            // action.disabled 只控制当前动作能否点，不影响整张入口卡展示。
            binding.tvSkip.isEnabled = action.disabled != true
            binding.tvSkip.setOnSingleClickListener { onInterviewNotNeedClickListener(item, action, payload) }
        } else {
            binding.tvSkip.gone()
        }
    }

    companion object {
        /** 服务端按钮禁用态，表示当前不可开始模拟面试。 */
        private const val BUTTON_STATUS_DISABLED = "DISABLED"

        /** 默认收费数量，保留给服务端未下发费用时兜底展示。 */
        private const val DEFAULT_CHARGE_AMOUNT = 40

        /** 默认收费单位，保留给服务端未下发单位时兜底展示。 */
        private const val DEFAULT_CHARGE_UNIT = "虾条"
    }
}
