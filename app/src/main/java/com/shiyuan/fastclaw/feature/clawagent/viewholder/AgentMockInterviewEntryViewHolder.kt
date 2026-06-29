package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.dp
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

    var onInterviewStartClickListener: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewEntryPayloadBean?) -> Unit = { _, _, _ -> }
    private var hasPlayedPersonEnterAnim = false
    private var currentMessageId: String? = null

    fun bind(item: ClawAgentMessageInfo) {
        val isNewCard = currentMessageId != item.id
        if (isNewCard) {
            currentMessageId = item.id
            preparePersonEnterAnim()
        }
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewEntryPayloadBean::class.java) }.getOrNull()
        }
        val context = binding.root.context
        val action = payload?.primaryAction ?: payload?.actions?.firstOrNull()
        binding.tvAction.text = binding.root.context.getString(R.string.str_agent_mock_interview_action_main)
        binding.tvCost.text = context.getString(R.string.str_agent_mock_interview_action_cost, payload?.chargeAmount?:0)
        binding.tvCost.visibility = View.VISIBLE
        val enabled = payload?.buttonStatus != "DISABLED"
        binding.layoutAction.isEnabled = enabled
        binding.layoutAction.setBackgroundResource(if (enabled) R.drawable.bg_mock_interview_entry_action else R.drawable.bg_mock_interview_entry_action_disabled)
        binding.tvAction.setTextColor(ContextCompat.getColor(context, if (enabled) R.color.color_ffffff else R.color.color_55_3c3c43))
        binding.tvCost.setTextColor(ContextCompat.getColor(context, if (enabled) R.color.color_ffffff else R.color.color_55_3c3c43))
        if (null == payload?.resumeId) {
            binding.tvAction.text = binding.root.context.getString(R.string.str_agent_mock_interview_action_upload)
            binding.tvCost.visibility = View.GONE
        }
        binding.layoutAction.setOnSingleClickListener {
            if (enabled) onInterviewStartClickListener(item, action, payload)
        }
        if (isNewCard && binding.root.isAttachedToWindow) {
            binding.ivPerson.post { startPersonEnterAnim() }
        }
    }

    fun startPersonEnterAnim() {
        if (hasPlayedPersonEnterAnim) return
        hasPlayedPersonEnterAnim = true
        binding.ivPerson.apply {
            clearAnimation()
            animate().cancel()
            alpha = 0f
            translationY = PERSON_ENTER_TRANSLATION_Y
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(PERSON_ENTER_DURATION)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun stopPersonEnterAnim() {
        binding.ivPerson.apply {
            clearAnimation()
            animate().cancel()
            alpha = 1f
            translationY = 0f
        }
    }

    private fun preparePersonEnterAnim() {
        hasPlayedPersonEnterAnim = false
        binding.ivPerson.apply {
            animate().cancel()
            alpha = 0f
            translationY = PERSON_ENTER_TRANSLATION_Y
        }
    }

    companion object {
        private const val PERSON_ENTER_DURATION = 520L
        private val PERSON_ENTER_TRANSLATION_Y = 64.dp.toFloat()
    }
}
