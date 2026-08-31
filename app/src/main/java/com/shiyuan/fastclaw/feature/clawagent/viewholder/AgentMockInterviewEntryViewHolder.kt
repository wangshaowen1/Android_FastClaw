package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewEntryPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewEntryBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentItemEvent
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 模拟面试入口卡
 */
class AgentMockInterviewEntryViewHolder(
    private val binding: ItemMsgAgentMockInterviewEntryBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    /** 人物入场动画是否已播放，避免列表上下滑动复用时重复播放。 */
    private var hasPlayedPersonEnterAnim = false

    /** 当前绑定的消息 ID，换成新卡片时重置入场动画状态。 */
    private var currentMessageId: String? = null

    /** 绑定模拟面试入口卡，并根据简历状态切换按钮文案和可点击状态。 */
    fun bind(item: ClawAgentMessageInfo) {
        val isNewCard = currentMessageId != item.id
        if (isNewCard) {
            currentMessageId = item.id
            // 新消息卡片首次出现前先放到动画起点，真正附着到窗口后再播放。
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
            // 没有可用简历时，入口按钮改成上传简历，不展示消耗次数。
            binding.tvAction.text = binding.root.context.getString(R.string.str_agent_mock_interview_action_upload)
            binding.tvCost.visibility = View.GONE
        }
        binding.layoutAction.setOnSingleClickListener {
            if (enabled) itemEvents.postEvent(ClawAgentItemEvent.InterviewStart(item, action, payload))
        }
        if (isNewCard && binding.root.isAttachedToWindow) {
            // 已经 attach 的新卡片，post 到下一帧确保起点状态先完成绘制。
            binding.ivPerson.post { startPersonEnterAnim() }
        }
    }

    /** 播放人物从下方淡入的入场动画。 */
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

    /** item 离屏或回收时停止动画，并恢复最终显示状态。 */
    fun stopPersonEnterAnim() {
        binding.ivPerson.apply {
            clearAnimation()
            animate().cancel()
            alpha = 1f
            translationY = 0f
        }
    }

    /** 新卡片绑定时重置人物图到动画起点。 */
    private fun preparePersonEnterAnim() {
        hasPlayedPersonEnterAnim = false
        binding.ivPerson.apply {
            animate().cancel()
            alpha = 0f
            translationY = PERSON_ENTER_TRANSLATION_Y
        }
    }

    companion object {
        /** 人物入场动画时长，单位毫秒。 */
        private const val PERSON_ENTER_DURATION = 520L

        /** 人物入场动画起始下移距离，单位 dp。 */
        private val PERSON_ENTER_TRANSLATION_Y = 64.dp.toFloat()
    }
}
