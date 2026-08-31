package com.shiyuan.fastclaw.feature.clawagent

import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundStartBody
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardOption
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewEntryPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageReportPayloadBean
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 小虾消息列表 item 事件。
 *
 * 事件参数与原 Adapter 回调保持一致；Adapter/ViewHolder 只负责转发，页面统一消费。
 */
sealed class ClawAgentItemEvent {

    data class UploadResume(
        val item: ClawAgentMessageInfo,
        val reUpload: Boolean,
    ) : ClawAgentItemEvent()

    data class QuestionOption(
        val item: ClawAgentMessageInfo,
        val option: CardOption,
        val questionQueueId: String?,
    ) : ClawAgentItemEvent()

    data class QuestionAction(
        val item: ClawAgentMessageInfo,
        val action: CardAction,
        val questionQueueId: String?,
    ) : ClawAgentItemEvent()

    data class QuestionInput(
        val item: ClawAgentMessageInfo,
        val action: CardAction,
        val questionQueueId: String?,
    ) : ClawAgentItemEvent()

    data class DefaultAction(
        val item: ClawAgentMessageInfo,
        val action: CardAction,
        val questionQueueId: String?,
    ) : ClawAgentItemEvent()

    data class ResumeConfirm(
        val item: ClawAgentMessageInfo,
        val confirmed: Boolean,
    ) : ClawAgentItemEvent()

    data class CompanyStart(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: CompanyBackgroundStartBody,
    ) : ClawAgentItemEvent()

    data class CompanyDone(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: CompanyBackgroundReportPayloadBean?,
    ) : ClawAgentItemEvent()

    data class CompanyRetry(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: CompanyBackgroundReportPayloadBean?,
    ) : ClawAgentItemEvent()

    data class InterviewStart(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: MockInterviewEntryPayloadBean?,
    ) : ClawAgentItemEvent()

    data class InterviewDone(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: MockInterviewReportPayloadBean?,
    ) : ClawAgentItemEvent()

    data class InterviewNotNeed(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: MockInterviewEntryPayloadBean?,
    ) : ClawAgentItemEvent()

    data class InterviewRetry(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: MockInterviewReportPayloadBean?,
    ) : ClawAgentItemEvent()

    data class PassPackageDone(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: PassPackageReportPayloadBean?,
    ) : ClawAgentItemEvent()

    data class PassPackageRetry(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: PassPackageReportPayloadBean?,
    ) : ClawAgentItemEvent()

    data class ActivityClick(
        val item: ClawAgentMessageInfo,
    ) : ClawAgentItemEvent()
}
