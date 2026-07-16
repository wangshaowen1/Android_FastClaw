package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardActionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_QUESTION_REANSWER_SUBMIT
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_SKIP_CURRENT_QUESTION
import com.shiyuan.fastclaw.databinding.ItemMsgAgentTableFormBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * @desc：表单相关{ARRAY_FORM} - 如工作经历、教育经历、技能、项目经历、证书、奖项
 * @author：CaoWei
 * @date：2026/6/11 10:51
 */
class AgentTableFormViewHolder(private val binding: ItemMsgAgentTableFormBinding) : RecyclerView.ViewHolder(binding.root) {

    /** 动作按钮点击回调（B 方案：仅暴露，提交后续统一接） */
    var onActionClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 动作按钮（如跳过）点击回调 */
    var onDefaultClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java) }.getOrNull()
        }
        payload?.let {
            // 本地验证用：服务端未下发时，临时补齐跳过/重新回答 action，方便验证重新回答按钮展示。
            val verifyPayload = payload.buildVerifyReAnswerPayload()
            binding.tvTitle.text = verifyPayload.questionText ?: verifyPayload.question ?: item.content
            bindActionRows(item, verifyPayload)
            bindSkipAction(item, verifyPayload)
            bindReAnswerAction(item, verifyPayload)
        }
    }

    private fun CoreQuestionPayloadBean.buildVerifyReAnswerPayload(): CoreQuestionPayloadBean {
        val currentActions = actions.orEmpty()
        val verifyActions = currentActions.toMutableList()
        val commonPayload = CardActionPayloadBean(
            questionQueueId = questionQueueId,
            attributeGroupCode = attributeGroupCode,
            metaCode = metaCode,
        )
        if (currentActions.none { it.actionCode == ACTION_SKIP_CURRENT_QUESTION }) {
            verifyActions.add(
                CardAction(
                    actionCode = ACTION_SKIP_CURRENT_QUESTION,
                    text = itemView.context.getString(R.string.skip),
                    disabled = false,
                    actionPayload = commonPayload,
                )
            )
        }
        if (currentActions.none { it.actionCode == ACTION_QUESTION_REANSWER_SUBMIT }) {
            verifyActions.add(
                CardAction(
                    actionCode = ACTION_QUESTION_REANSWER_SUBMIT,
                    text = itemView.context.getString(R.string.reanswer),
                    disabled = false,
                    actionPayload = commonPayload,
                )
            )
        }
        return copy(actions = verifyActions)
    }

    private fun bindActionRows(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull()
        action?.let {
            val parent = binding.layoutTableCard
            //binding.btnName.text = action.text
            if (payload.isAnsweredV2()) { //已回答
                binding.tvContent.text = parent.context.getString(R.string.already_write)
                binding.tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
                binding.tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff))
                binding.flowOptions.setBackgroundResource(R.drawable.bg_option_selected)
            } else if (action.disabled == true) { //置灰
                //binding.tvContent.text = parent.context.getString(R.string.please_write)
                binding.tvContent.text = action.text
                binding.tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                binding.tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_55_3c3c43))
                binding.flowOptions.setBackgroundResource(R.drawable.bg_option_unselected)
            } else { //未回答
                //binding.tvContent.text = parent.context.getString(R.string.please_write)
                binding.tvContent.text = action.text
                binding.tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
                binding.tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_75_3c3c43))
                binding.flowOptions.setBackgroundResource(R.drawable.bg_option_ripple)
            }
            binding.flowOptions.isEnabled = payload.isDisabled() != true
            //binding.btnLayout.isEnabled = true
            binding.flowOptions.setOnSingleClickListener {
                action.actionCode = CandidateJobAgentClientActionEnum.QUESTION_OPTION_SUBMIT.name
                onActionClickListener(item, action, payload.questionQueueId)
            }
        }
    }

    /**
     * 重新回答 匹配 QUESTION_REANSWER_SUBMIT
     */
    private fun bindReAnswerAction(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_QUESTION_REANSWER_SUBMIT }
        if (action == null || item.isReplyAnswer) {
            binding.flowOptions.visible()
            binding.tvReanswer.gone()
            binding.tvReanswer.setOnClickListener(null)
            return
        }
        binding.flowOptions.gone()
        binding.tvReanswer.visible()
        binding.tvReanswer.text = action.text
        binding.tvReanswer.isEnabled = action.disabled != true
        binding.tvReanswer.setOnSingleClickListener {
            item.isReplyAnswer = true
            binding.flowOptions.visible()
            binding.tvReanswer.gone()
        }
    }

    /** 绑定独立跳过按钮：优先匹配 SKIP_CURRENT_QUESTION，回退到第一个 action；无 action 则隐藏 */
    private fun bindSkipAction(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_SKIP_CURRENT_QUESTION }
        if (action != null && !payload.isAnsweredV2() && !payload.isDisabled()) {
            binding.tvSkip.visible()
            binding.tvSkip.isEnabled = action.disabled != true
            binding.tvSkip.setOnSingleClickListener { onDefaultClickListener(item, action, payload.questionQueueId) }
        } else {
            binding.tvSkip.gone()
        }
    }
}
