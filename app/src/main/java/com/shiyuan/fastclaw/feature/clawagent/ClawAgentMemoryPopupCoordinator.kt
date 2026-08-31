package com.shiyuan.fastclaw.feature.clawagent

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SpanUtils
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.request.AgentMsgAction
import com.shiyuan.fastclaw.core.network.apis.response.AttributeConfirmPayloadBean
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.feature.mine.OnlineResumeActivity
import com.shiyuan.fastclaw.router.AppNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 记忆确认 PopupWindow 的渲染、动作回传与自动确认。 */
internal class ClawAgentMemoryPopupCoordinator(
    private val fragment: ClawAgentMainFragment,
    private val viewModel: ClawAgentViewModel,
    private val onActionStarted: () -> Unit,
) {

    private var popup: PopupWindow? = null
    private var autoConfirmJob: Job? = null

    fun show(item: ClawAgentMessageInfo, anchor: View) {
        dismiss()
        val payload = item.metaData?.let {
            runCatching {
                GsonUtils.fromJson(it, AttributeConfirmPayloadBean::class.java)
            }.getOrNull()
        }
        val contentView = LayoutInflater.from(fragment.context)
            .inflate(R.layout.popup_memory_card_select, null, false)
        val contentText = contentView.findViewById<TextView>(R.id.tv_content)
        val confirmButton = contentView.findViewById<TextView>(R.id.tv_confirm)
        val rejectButton = contentView.findViewById<TextView>(R.id.tv_negation)
        val allMemoryButton = contentView.findViewById<TextView>(R.id.tv_all_memory)

        bindContent(contentText, payload)
        bindButtonText(confirmButton, rejectButton, payload)
        allMemoryButton.setOnSingleClickListener {
            if (!AppNavigator.checkLoginIntercepted(false)) {
                OnlineResumeActivity.start(fragment.requireActivity(), 0)
            }
        }

        confirmButton.setOnClickListener {
            submit(item, payload, confirmed = true)
        }
        rejectButton.setOnClickListener {
            submit(item, payload, confirmed = false)
        }

        contentView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        popup = PopupWindow(fragment.context).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            this.contentView = contentView
            width = ScreenUtils.getScreenWidth()
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            isFocusable = false
            isOutsideTouchable = false
            showAtLocation(anchor, Gravity.TOP, 0, 0)
        }

        autoConfirmJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
            delay(BaseClawAgentFragment.MEMORY_CARD_WAIT_TIME)
            confirmButton.performClick()
        }
    }

    private fun bindContent(contentText: TextView, payload: AttributeConfirmPayloadBean?) {
        val spans = SpanUtils.with(contentText)
        val confirmationItems = payload?.confirmationItems.orEmpty()
        if (confirmationItems.isEmpty()) {
            spans.append("${payload?.fieldName}：")
                .append("${payload?.displayText}")
                .setForegroundColor(R.color.color_90_000000.getResColor())
                .setBold()
        } else {
            confirmationItems.forEachIndexed { index, item ->
                spans.append("${item.fieldName}：")
                    .append("${item.displayText}")
                    .setForegroundColor(R.color.color_90_000000.getResColor())
                    .setBold()
                if (index < confirmationItems.lastIndex) spans.append("\n")
            }
        }
        spans.create()
    }

    private fun bindButtonText(
        confirmButton: TextView,
        rejectButton: TextView,
        payload: AttributeConfirmPayloadBean?,
    ) {
        payload?.actions?.forEach { action ->
            when (action.actionCode) {
                CandidateJobAgentClientActionEnum.ATTRIBUTE_CONFIRM.name -> {
                    confirmButton.text =
                        "${action.text ?: R.string.str_agent_memory_confirm.getResString()}"
                }

                CandidateJobAgentClientActionEnum.ATTRIBUTE_REJECT.name -> {
                    rejectButton.text =
                        "${action.text ?: R.string.str_agent_memory_negation.getResString()}"
                }
            }
        }
    }

    private fun submit(
        item: ClawAgentMessageInfo,
        payload: AttributeConfirmPayloadBean?,
        confirmed: Boolean,
    ) {
        onActionStarted()
        val text = (
            if (confirmed) R.string.str_agent_memory_confirm
            else R.string.str_agent_memory_negation
            ).getResString()
        viewModel.sendAction(
            content = text,
            actionData = AgentMsgAction(
                actionCode = (
                    if (confirmed) CandidateJobAgentClientActionEnum.ATTRIBUTE_CONFIRM
                    else CandidateJobAgentClientActionEnum.ATTRIBUTE_REJECT
                    ).name,
                sourceMessageId = item.id,
                questionQueueId = payload?.questionQueueId,
                actionPayloadJson = buildActionPayload(payload, text),
            )
        )
        dismiss()
    }

    private fun buildActionPayload(
        payload: AttributeConfirmPayloadBean?,
        selectedText: String,
    ): String {
        val values = buildMap {
            payload?.questionQueueId?.let { put("questionQueueId", it) }
            payload?.attributeGroupCode?.let { put("attributeGroupCode", it) }
            payload?.metaCode?.let { put("metaCode", it) }
            payload?.confirmMode?.let { put("confirmMode", it) }
            put("selectedText", selectedText)
        }
        return GsonUtils.toJson(values)
    }

    private fun dismiss() {
        autoConfirmJob?.cancel()
        autoConfirmJob = null
        popup?.dismiss()
        popup = null
    }
}
