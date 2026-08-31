package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_QUESTION_REANSWER_SUBMIT
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_SKIP_CURRENT_QUESTION
import com.shiyuan.fastclaw.databinding.ItemMsgAgentTableFormBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentItemEvent
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * @desc：表单相关{ARRAY_FORM} - 如工作经历、教育经历、技能、项目经历、证书、奖项
 * @author：CaoWei
 * @date：2026/6/11 10:51
 */
class AgentTableFormViewHolder(
    private val binding: ItemMsgAgentTableFormBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java) }.getOrNull()
        }
        payload?.let {
            binding.tvTitle.text = payload.questionText ?: payload.question ?: item.content
            bindActionRows(item, payload)
            bindSkipAction(item, payload)
            bindReAnswerAction(item, payload)
            bindButtonVisible(item, payload, payload.actions?.size?:0)
        }
    }

    private fun bindActionRows(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode != ACTION_SKIP_CURRENT_QUESTION }
        action?.let {
            val parent = binding.layoutTableCard
            if (payload.isAnswered()) { //已回答
                binding.tvContent.text = parent.context.getString(R.string.already_write)
                binding.tvContent.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
                binding.tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff))
                binding.flowOptions.setBackgroundResource(R.drawable.bg_option_selected)
            } else if (action.disabled == true) { //置灰
                binding.tvContent.text = action.text ?: parent.context.getString(R.string.please_write)
                binding.tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                binding.tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_55_3c3c43))
                binding.flowOptions.setBackgroundResource(R.drawable.bg_option_unselected)
            } else { //未回答
                binding.tvContent.text = action.text ?: parent.context.getString(R.string.please_write)
                binding.tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                binding.tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_75_3c3c43))
                binding.flowOptions.setBackgroundResource(R.drawable.bg_option_ripple)
            }
            binding.ivInto.visible()
            binding.flowOptions.isEnabled = payload.isDisabled() != true
            binding.flowOptions.setOnSingleClickListener {
                action.actionCode = CandidateJobAgentClientActionEnum.QUESTION_OPTION_SUBMIT.name
                itemEvents.postEvent(ClawAgentItemEvent.QuestionAction(item, action, payload.questionQueueId))
            }
        }
    }

    /**
     * 重新回答 匹配 QUESTION_REANSWER_SUBMIT
     */
    private fun bindReAnswerAction(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_QUESTION_REANSWER_SUBMIT }
        if (action == null) {
            binding.tvReanswer.gone()
            binding.tvReanswer.setOnClickListener(null)
            return
        }
        binding.tvReanswer.text = action.text
        binding.tvReanswer.isEnabled = action.disabled != true
        binding.tvReanswer.setOnSingleClickListener {
            action.actionCode = CandidateJobAgentClientActionEnum.QUESTION_REANSWER_SUBMIT.name
            itemEvents.postEvent(ClawAgentItemEvent.QuestionAction(item, action, payload.questionQueueId))
        }
    }

    /**
     * 设置卡片上控件的显示状态
     */
    private fun bindButtonVisible(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?, actionCount: Int) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_QUESTION_REANSWER_SUBMIT }
        if (actionCount > 0 && (payload?.isAnswered() == false || item.isReplyAnswer)) {
            binding.flowOptions.visible()
        } else {
            binding.flowOptions.gone()
        }
        if (action != null && payload.isAnswered() && action.disabled != true && !item.isReplyAnswer) {
            binding.tvReanswer.visible()
        } else {
            binding.tvReanswer.setOnClickListener(null)
            binding.tvReanswer.gone()
        }
        if (payload?.isAnswered() == true || payload?.isDisabled() == true) {
            binding.tvSkip.gone()
        }
    }

    /**
     * 绑定独立跳过按钮：优先匹配 SKIP_CURRENT_QUESTION，回退到第一个 action；无 action 则隐藏
     */
    private fun bindSkipAction(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_SKIP_CURRENT_QUESTION }

        if (action == null) {
            binding.tvSkip.gone()
            binding.tvSkip.setOnClickListener(null)
            return
        }
        binding.tvSkip.visible()
        binding.tvSkip.isEnabled = action.disabled != true
        binding.tvSkip.setOnSingleClickListener {
            itemEvents.postEvent(ClawAgentItemEvent.DefaultAction(item, action, payload.questionQueueId))
        }
    }
}
