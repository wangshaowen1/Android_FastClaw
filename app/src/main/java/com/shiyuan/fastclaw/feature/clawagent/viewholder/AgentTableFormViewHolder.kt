package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_SKIP_CURRENT_QUESTION
import com.shiyuan.fastclaw.databinding.ItemMsgAgentTableFormBinding
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
    var onSkipClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java) }.getOrNull()
        }
        payload?.let {
            binding.tvTitle.text = payload.questionText ?: payload.question ?: item.content
            bindActionRows(item, payload)
            bindSkipAction(item, payload)
        }
    }

    private fun bindActionRows(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        if (payload?.actions?.isNotEmpty() == true) {
            val parent = binding.layoutTableCard
            val action = payload.actions[0]
            //binding.btnName.text = action.text
            if (payload.isAnsweredV2()) { //已回答
                binding.btnName.text = parent.context.getString(R.string.already_write)
                binding.btnName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff))
                binding.btnLayout.setBackgroundResource(R.drawable.bg_option_selected)
            } else if (action.disabled == true) { //置灰
                binding.btnName.text = parent.context.getString(R.string.please_write)
                binding.btnName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_55_3c3c43))
                binding.btnLayout.setBackgroundResource(R.drawable.bg_option_unselected)
            } else { //未回答
                binding.btnName.text = parent.context.getString(R.string.please_write)
                binding.btnName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_ffffff))
                binding.btnLayout.setBackgroundResource(R.drawable.bg_upload_resume_btn)
            }
            binding.btnLayout.isEnabled = payload.isDisabled() != true
            binding.btnLayout.setOnClickListener { onActionClickListener(item, action, payload.questionQueueId) }
        }
    }

    /** 绑定独立跳过按钮：优先匹配 SKIP_CURRENT_QUESTION，回退到第一个 action；无 action 则隐藏 */
    private fun bindSkipAction(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_SKIP_CURRENT_QUESTION }
        if (action != null) {
            binding.tvSkip.visible()
            binding.tvSkip.isEnabled = action.disabled != true
            binding.tvSkip.setOnClickListener { onSkipClickListener(item, action, payload.questionQueueId) }
        } else {
            binding.tvSkip.gone()
        }
    }
}