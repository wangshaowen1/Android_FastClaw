package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardOption
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_QUESTION_REANSWER_SUBMIT
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.ACTION_SKIP_CURRENT_QUESTION
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.MODE_DATE_MONTH
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.MODE_DICT_MULTI
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.MODE_DICT_SINGLE
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.MODE_MULTI_CHOICE
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.MODE_SALARY_RANGE
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.MODE_SINGLE_CHOICE
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean.Companion.MODE_TEXT
import com.shiyuan.fastclaw.databinding.ItemMsgAgentQuestionBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent核心补问卡（CORE_QUESTION：问题 + 选项 + 已回复回显）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentQuestionViewHolder(private val binding: ItemMsgAgentQuestionBinding) : RecyclerView.ViewHolder(binding.root) {

    /** 选项点击回调（B 方案：仅暴露，提交由后续统一接 sendAction(QUESTION_OPTION_SUBMIT)） */
    var onOptionClickListener: (item: ClawAgentMessageInfo, option: CardOption, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 动作按钮点击回调（B 方案：仅暴露，提交后续统一接） */
    var onActionClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 动作按钮（如跳过）点击回调 */
    var onDefaultClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 点击拉起输入法 */
    var onInputClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 上次动态添加的选项/动作行，复用前先移除 */
    private val optionRows = ArrayList<View>()

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java) }.getOrNull()
        }
        binding.tvQuestion.text = payload?.questionText ?: payload?.question ?: item.content
        when (payload?.inputMode) {
            MODE_DICT_SINGLE, MODE_DICT_MULTI, MODE_DATE_MONTH, MODE_SALARY_RANGE -> { //单选/多选字典、表单问题等
                binding.tvSkip.gone()
                binding.tvReplied.gone()
                bindActionRows(item, payload)
                bindReAnswerAction(item, payload)
                bindButtonVisible(item, payload)
            }
            else -> { //单选/多选枚举、文本、通用问题等
                bindOptionRows(item, payload)
                bindReAnswerAction(item, payload)
                bindSkipAction(item, payload)
                bindButtonVisible(item, payload)
            }
        }
    }

    /**
     * 渲染选项到 Flow：先移除旧行，再 inflate + addView，最后接入 flow.referencedIds
     */
    private fun bindOptionRows(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        clearOptionRows()
        val parent = binding.layoutQuestionCard
        val inflater = LayoutInflater.from(parent.context)
        val ids = ArrayList<Int>()
        payload?.options?.forEach { option ->
            val view = inflater.inflate(R.layout.item_agent_question_option, parent, false)
            val tvContent = view.findViewById<TextView>(R.id.tv_content)
            if (payload.isAnswered() && option.selected == true) { //已回答 & 已选择
                tvContent.setBackgroundResource(R.drawable.bg_option_selected)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff))
            } else if (option.disabled == true) { //置灰
                tvContent.setBackgroundResource(R.drawable.bg_option_unselected)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_55_3c3c43))
            } else {
                tvContent.setBackgroundResource(R.drawable.bg_option_ripple)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_90_000000))
            }
            tvContent.text = option.text ?: option.label.orEmpty()
            //tvContent.isEnabled = option.disabled != true
            tvContent.setOnSingleClickListener(700L) {
                option.actionCode = if (payload.isAnswered()) CandidateJobAgentClientActionEnum.QUESTION_REANSWER_SUBMIT.name else option.actionCode
                onOptionClickListener(item, option, payload.questionQueueId)
            }
            view.id = View.generateViewId()
            parent.addView(view)
            ids.add(view.id)
            optionRows.add(view)
        }
        binding.flowOptions.referencedIds = ids.toIntArray()
    }

    private fun bindActionRows(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        clearOptionRows()
        val parent = binding.layoutQuestionCard
        val inflater = LayoutInflater.from(parent.context)
        val ids = ArrayList<Int>()
        payload?.let {
            val view = inflater.inflate(R.layout.item_agent_question_option, parent, false)
            val tvContent = view.findViewById<TextView>(R.id.tv_content)
            val ivInto = view.findViewById<ImageView>(R.id.iv_into)
            if (payload.isAnswered()) { //已回答
                tvContent.text = parent.context.getString(R.string.already_selected)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD))
                tvContent.setBackgroundResource(R.drawable.bg_option_selected)
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context,R.color.color_2f6bff))
            } else if (payload.isDisabled()) { //置灰
                tvContent.text = parent.context.getString(R.string.please_selected)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                tvContent.setBackgroundResource(R.drawable.bg_option_unselected)
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context,R.color.color_55_3c3c43))
            } else { //未回答
                tvContent.text = parent.context.getString(R.string.please_selected)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                tvContent.setBackgroundResource(R.drawable.bg_option_ripple)
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context,R.color.color_75_3c3c43))
            }
            ivInto.visible()
            tvContent.isEnabled = payload.isDisabled() != true
            tvContent.setOnSingleClickListener {
                onActionClickListener(item, payload.buildAction(CandidateJobAgentClientActionEnum.QUESTION_OPTION_SUBMIT), payload.questionQueueId)
            }
            view.id = View.generateViewId()
            parent.addView(view)
            ids.add(view.id)
            optionRows.add(view)
        }
        binding.flowOptions.referencedIds = ids.toIntArray()
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
            if (payload.inputMode == MODE_SINGLE_CHOICE || payload.inputMode == MODE_MULTI_CHOICE) { //单选/多选枚举
                binding.flowOptions.visible()
                binding.tvReanswer.gone()
                item.isReplyAnswer = true
            } else if (payload.inputMode == MODE_TEXT) { //文本
                onInputClickListener(item, action, payload.questionQueueId)
            } else {
                onActionClickListener(item, payload.buildAction(CandidateJobAgentClientActionEnum.QUESTION_REANSWER_SUBMIT), payload.questionQueueId)
            }
        }
    }

    /**
     * 绑定独立跳过按钮：优先匹配 SKIP_CURRENT_QUESTION，回退到第一个 action；无 action 则隐藏
     */
    private fun bindSkipAction(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_SKIP_CURRENT_QUESTION }

        val center = payload?.inputMode in setOf(MODE_SINGLE_CHOICE, MODE_MULTI_CHOICE)
        val layoutParams = binding.tvSkip.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.horizontalBias = if (center) 0.5f else 1f
        binding.tvSkip.layoutParams = layoutParams
        if (action == null) {
            binding.tvSkip.gone()
            binding.tvSkip.setOnClickListener(null)
            return
        }
        binding.tvSkip.visible()
        binding.tvSkip.isEnabled = action.disabled != true
        binding.tvSkip.setOnSingleClickListener { onDefaultClickListener(item, action, payload.questionQueueId) }
    }

    /**
     * 设置卡片上控件的显示状态
     */
    private fun bindButtonVisible(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_QUESTION_REANSWER_SUBMIT }
        if (payload?.inputMode != MODE_TEXT && (payload?.isAnswered() == false || item.isReplyAnswer)) {
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

    private fun clearOptionRows() {
        val parent = binding.layoutQuestionCard
        optionRows.forEach { parent.removeView(it) }
        optionRows.clear()
        binding.flowOptions.referencedIds = IntArray(0)
    }
}
