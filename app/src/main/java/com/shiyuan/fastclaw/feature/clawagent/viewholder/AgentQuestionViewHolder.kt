package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardOption
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentQuestionBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent核心补问卡（CORE_QUESTION：问题 + 选项 + 已回复回显）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentQuestionViewHolder(private val binding: ItemMsgAgentQuestionBinding) : RecyclerView.ViewHolder(binding.root) {

    /** 选项点击回调（B 方案：仅暴露，提交由后续统一接 sendAction(QUESTION_OPTION_SUBMIT)） */
    var onOptionClickListener: (item: ClawAgentMessageInfo, option: CardOption, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 动作按钮（如跳过）点击回调（B 方案：仅暴露，提交后续统一接） */
    var onActionClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 上次动态添加的选项/动作行，复用前先移除 */
    private val optionRows = ArrayList<View>()

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java) }.getOrNull()
        }
        binding.tvQuestion.text = payload?.questionText ?: payload?.question ?: item.content
        if (payload.isAnswered()) {
            // 已回复：隐藏选项，展示回显
            clearOptionRows()
            binding.flowOptions.gone()
            binding.tvReplied.visible()
            val repliedText = payload?.answeredText
                ?: payload?.interactionState?.selectedText
                ?: ""
            binding.tvReplied.text = "${repliedText}"
        } else {
            // 未回复：展示选项 + 动作按钮
            binding.tvReplied.gone()
            binding.flowOptions.visibility = if (payload?.options?.isEmpty() == true) {
                View.GONE
            } else {
                View.VISIBLE
            }
            bindOptionRows(item, payload)
        }
    }

    /** 整卡是否已回答：状态机终态或整卡禁用 */
    private fun CoreQuestionPayloadBean?.isAnswered(): Boolean {
        val state = this?.interactionState ?: return false
        return state.disabled == true ||
            state.status == STATUS_ANSWERED ||
            state.status == STATUS_SUBMITTED ||
            state.status == STATUS_SKIPPED
    }

    /** 渲染选项 + 动作按钮到 Flow：先移除旧行，再 inflate + addView，最后接入 flow.referencedIds */
    private fun bindOptionRows(item: ClawAgentMessageInfo, payload: CoreQuestionPayloadBean?) {
        val parent = binding.clQuestionCard
        clearOptionRows()
        val inflater = LayoutInflater.from(parent.context)
        val ids = ArrayList<Int>()
        payload?.options?.forEach { option ->
            val view = inflater.inflate(R.layout.item_agent_question_option, parent, false) as TextView
            view.text = option.text ?: option.label.orEmpty()
            view.isEnabled = option.disabled != true
            view.id = View.generateViewId()
            view.setOnClickListener { onOptionClickListener(item, option, payload.questionQueueId) }
            parent.addView(view)
            ids.add(view.id)
            optionRows.add(view)
        }
        // 动作按钮（如跳过）布局无专用位置，渲染到选项流末尾
        payload?.actions?.forEach { action ->
            val view = inflater.inflate(R.layout.item_agent_question_option, parent, false) as TextView
            view.text = action.text.orEmpty()
            view.isEnabled = action.disabled != true
            view.id = View.generateViewId()
            view.setOnClickListener { onActionClickListener(item, action, payload.questionQueueId) }
            parent.addView(view)
            ids.add(view.id)
            optionRows.add(view)
        }
        binding.flowOptions.referencedIds = ids.toIntArray()
    }

    private fun clearOptionRows() {
        val parent = binding.clQuestionCard
        optionRows.forEach { parent.removeView(it) }
        optionRows.clear()
        binding.flowOptions.referencedIds = IntArray(0)
    }

    companion object {
        private const val STATUS_ANSWERED = "ANSWERED"
        private const val STATUS_SUBMITTED = "SUBMITTED"
        private const val STATUS_SKIPPED = "SKIPPED"
    }

}