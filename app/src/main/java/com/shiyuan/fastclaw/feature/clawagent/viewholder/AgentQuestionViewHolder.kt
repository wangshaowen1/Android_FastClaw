package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.animation.ValueAnimator
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
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
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentItemEvent
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent核心补问卡（CORE_QUESTION：问题 + 选项 + 已回复回显）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentQuestionViewHolder(
    private val binding: ItemMsgAgentQuestionBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    /** 上次动态添加的选项/动作行，复用前先移除 */
    private val optionRows = ArrayList<View>()

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java) }.getOrNull()
        }
        var center : Boolean = false
        binding.tvQuestion.text = payload?.questionText ?: payload?.question ?: item.content
        when (payload?.inputMode) {
            MODE_DICT_SINGLE, MODE_DICT_MULTI, MODE_DATE_MONTH, MODE_SALARY_RANGE -> { //单选/多选字典、薪资、年月问题等
                binding.tvReplied.gone()
                center = true
                bindActionRows(item, payload)
            }
            else -> { //单选/多选枚举、文本、通用问题等
                center = payload?.inputMode in setOf(MODE_SINGLE_CHOICE, MODE_MULTI_CHOICE)
                bindOptionRows(item, payload)
            }
        }
        bindReAnswerAction(item, payload)
        bindSkipAction(item, payload, center)
        bindButtonVisible(item, payload)
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
                tvContent.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff))
            } else if (option.disabled == true) { //置灰
                tvContent.setBackgroundResource(R.drawable.bg_option_unselected)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_55_3c3c43))
            } else {
                tvContent.setBackgroundResource(R.drawable.bg_option_ripple)
                tvContent.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_90_000000))
            }
            tvContent.text = option.text ?: option.label.orEmpty()
            //tvContent.isEnabled = option.disabled != true
            tvContent.setOnSingleClickListener(700L) {
                option.actionCode = if (payload.isAnswered()) CandidateJobAgentClientActionEnum.QUESTION_REANSWER_SUBMIT.name else option.actionCode
                itemEvents.postEvent(ClawAgentItemEvent.QuestionOption(item, option, payload.questionQueueId))
            }
            view.id = View.generateViewId()
            parent.addView(view)
            ids.add(view.id)
            optionRows.add(view)
        }
        binding.flowOptions.referencedIds = ids.toIntArray()
    }

    private fun bindActionRows(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val action = payload?.actions?.firstOrNull { it.actionCode != ACTION_SKIP_CURRENT_QUESTION }
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
                tvContent.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
                tvContent.setBackgroundResource(R.drawable.bg_option_selected)
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context,R.color.color_2f6bff))
            } else if (payload.isDisabled()) { //置灰
                tvContent.text = action?.text ?: parent.context.getString(R.string.please_selected)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                tvContent.setBackgroundResource(R.drawable.bg_option_unselected)
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context,R.color.color_55_3c3c43))
            } else { //未回答
                tvContent.text = action?.text ?: parent.context.getString(R.string.please_selected)
                tvContent.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
                tvContent.setBackgroundResource(R.drawable.bg_option_ripple)
                tvContent.setTextColor(ContextCompat.getColor(binding.root.context,R.color.color_75_3c3c43))
            }
            ivInto.visible()
            tvContent.isEnabled = payload.isDisabled() != true
            tvContent.setOnSingleClickListener {
                itemEvents.postEvent(
                    ClawAgentItemEvent.QuestionAction(
                        item,
                        payload.buildAction(CandidateJobAgentClientActionEnum.QUESTION_OPTION_SUBMIT),
                        payload.questionQueueId,
                    )
                )
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
                expandReAnswerOptions(item)
            } else if (payload.inputMode == MODE_TEXT) { //文本
                itemEvents.postEvent(ClawAgentItemEvent.QuestionInput(item, action, payload.questionQueueId))
            } else {
                itemEvents.postEvent(
                    ClawAgentItemEvent.QuestionAction(
                        item,
                        payload.buildAction(CandidateJobAgentClientActionEnum.QUESTION_REANSWER_SUBMIT),
                        payload.questionQueueId,
                    )
                )
            }
        }
    }

    /**
     * 绑定独立跳过按钮：优先匹配 SKIP_CURRENT_QUESTION，无 action 则隐藏。
     */
    private fun bindSkipAction(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?, center: Boolean = false) {
        val action = payload?.actions?.firstOrNull { it.actionCode == ACTION_SKIP_CURRENT_QUESTION }

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
        binding.tvSkip.setOnSingleClickListener {
            itemEvents.postEvent(ClawAgentItemEvent.DefaultAction(item, action, payload.questionQueueId))
        }
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

    /** 重新回答展开时用高度动画逐步撑开选项，并固定当前 item 顶部位置。 */
    private fun expandReAnswerOptions(item: ClawAgentMessageInfo) {
        val recyclerView = itemView.parent as? RecyclerView
        // 记录当前 adapter 位置，动画过程中用它把当前卡片固定在原来的屏幕位置。
        val position = bindingAdapterPosition
        // 记录展开前 item 顶部位置，避免 RecyclerView 贴底时把卡片向上顶。
        val oldTop = itemView.top
        // 选项样式一致，只测第一条即可拿到动态高度，避免写死高度。
        val targetHeight = optionRows.firstOrNull()?.let { row ->
            row.measure(
                View.MeasureSpec.makeMeasureSpec(binding.flowOptions.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            row.measuredHeight
        } ?: 44.dp
        // 先把选项压成 0 高度，后面用动画从上往下撑开。
        optionRows.forEach { row ->
            row.layoutParams = row.layoutParams.apply { height = 0 }
            row.alpha = 0f
        }
        binding.flowOptions.visible()
        binding.tvReanswer.gone()
        // 标记当前问题卡已进入重新回答态，后续 bind 时直接展示选项而不是“重新回答”按钮。
        item.isReplyAnswer = true
        val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager
        optionRows.forEach { row ->
            ValueAnimator.ofInt(0, targetHeight).apply {
                duration = 160L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    // 同步改变高度和透明度，形成从上往下撑开的展开效果。
                    row.layoutParams = row.layoutParams.apply { height = animator.animatedValue as Int }
                    row.alpha = animator.animatedFraction
                    if (position != RecyclerView.NO_POSITION && recyclerView != null) {
                        // 动画过程中持续固定当前 item 顶部，让展开方向保持向下。
                        layoutManager?.scrollToPositionWithOffset(position, oldTop - recyclerView.paddingTop)
                    }
                }
                start()
            }
        }
    }

    private fun clearOptionRows() {
        val parent = binding.layoutQuestionCard
        optionRows.forEach { parent.removeView(it) }
        optionRows.clear()
        binding.flowOptions.referencedIds = IntArray(0)
    }
}
