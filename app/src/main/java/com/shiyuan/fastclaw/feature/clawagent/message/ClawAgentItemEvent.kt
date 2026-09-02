package com.shiyuan.fastclaw.feature.clawagent.message

import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundStartBody
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardOption
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewEntryPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageReportPayloadBean

/**
 * 小虾消息列表 item 事件。
 *
 * 事件参数与原 Adapter 回调保持一致；Adapter/ViewHolder 只负责转发，页面统一消费。
 */
sealed class ClawAgentItemEvent {

    /** 上传或重新上传简历。 */
    data class UploadResume(
        val item: ClawAgentMessageInfo,
        val reUpload: Boolean,
    ) : ClawAgentItemEvent()

    /** 选择补问卡选项。 */
    data class QuestionOption(
        val item: ClawAgentMessageInfo,
        val option: CardOption,
        val questionQueueId: String?,
    ) : ClawAgentItemEvent()

    /** 点击补问卡动作按钮。 */
    data class QuestionAction(
        val item: ClawAgentMessageInfo,
        val action: CardAction,
        val questionQueueId: String?,
    ) : ClawAgentItemEvent()

    /** 点击补问卡文本输入入口。 */
    data class QuestionInput(
        val item: ClawAgentMessageInfo,
        val action: CardAction,
        val questionQueueId: String?,
    ) : ClawAgentItemEvent()

    /** 点击卡片通用动作按钮。 */
    data class DefaultAction(
        val item: ClawAgentMessageInfo,
        val action: CardAction,
        val questionQueueId: String?,
    ) : ClawAgentItemEvent()

    /** 确认或否认历史简历。 */
    data class ResumeConfirm(
        val item: ClawAgentMessageInfo,
        val confirmed: Boolean,
    ) : ClawAgentItemEvent()

    /** 开始生成公司背调报告。 */
    data class CompanyStart(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: CompanyBackgroundStartBody,
    ) : ClawAgentItemEvent()

    /** 查看公司背调报告。 */
    data class CompanyDone(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: CompanyBackgroundReportPayloadBean?,
    ) : ClawAgentItemEvent()

    /** 重新生成公司背调报告。 */
    data class CompanyRetry(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: CompanyBackgroundReportPayloadBean?,
    ) : ClawAgentItemEvent()

    /** 开始模拟面试。 */
    data class InterviewStart(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: MockInterviewEntryPayloadBean?,
    ) : ClawAgentItemEvent()

    /** 查看模拟面试报告。 */
    data class InterviewDone(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: MockInterviewReportPayloadBean?,
    ) : ClawAgentItemEvent()

    /** 暂不进行模拟面试。 */
    data class InterviewNotNeed(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: MockInterviewEntryPayloadBean?,
    ) : ClawAgentItemEvent()

    /** 重新生成模拟面试报告。 */
    data class InterviewRetry(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: MockInterviewReportPayloadBean?,
    ) : ClawAgentItemEvent()

    /** 查看面试通关包报告。 */
    data class PassPackageDone(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: PassPackageReportPayloadBean?,
    ) : ClawAgentItemEvent()

    /** 重新生成面试通关包报告。 */
    data class PassPackageRetry(
        val item: ClawAgentMessageInfo,
        val action: CardAction?,
        val data: PassPackageReportPayloadBean?,
    ) : ClawAgentItemEvent()

    /** 点击活动卡片。 */
    data class ActivityClick(
        val item: ClawAgentMessageInfo,
    ) : ClawAgentItemEvent()
}
