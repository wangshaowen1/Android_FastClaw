package com.shiyuan.fastclaw.feature.clawagent

import com.nhsd.basic.bus.FlowEventBus
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.SalaryRangePayload
import com.shiyuan.fastclaw.feature.information.IndustrySelectionActivity
import com.shiyuan.fastclaw.feature.information.JobCitySelectorActivity
import com.shiyuan.fastclaw.feature.information.JobSelectionActivity
import com.shiyuan.fastclaw.feature.information.JobWelfareSelectorActivity
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.CertificateSkillsAwardsDialog
import com.shiyuan.fastclaw.feature.information.dialog.EducationExperienceDialog
import com.shiyuan.fastclaw.feature.information.dialog.ProjectExperienceDialog
import com.shiyuan.fastclaw.feature.information.dialog.SalaryRangePickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.WorkExperienceDialog

/** 负责补问卡的页面选择器与表单弹窗路由。 */
internal class ClawAgentQuestionActionCoordinator(
    private val fragment: ClawAgentMainFragment,
    private val viewModel: ClawAgentViewModel,
) {

    /** 返回 true 表示已由选择器或弹窗处理，false 时由页面走普通消息动作。 */
    fun handle(payload: CoreQuestionPayloadBean?, questionQueueId: String?): Boolean {
        if (payload == null) return false
        when {
            payload.isLocationCity() -> {
                JobCitySelectorActivity.start(fragment.activity, JobCityQueryBody.LOCATION)
            }

            payload.isExpectedCity() -> {
                JobCitySelectorActivity.start(fragment.activity, JobCityQueryBody.EXPECTED_CITY)
            }

            payload.isJobIntention() -> {
                JobSelectionActivity.start(
                    fragment.activity,
                    true,
                    viewModel.sessionId,
                    viewModel.conversationId,
                    multipleNum = 10,
                )
            }

            payload.isExpectedIndustry() -> {
                IndustrySelectionActivity.start(
                    fragment.activity,
                    true,
                    viewModel.sessionId,
                    viewModel.conversationId,
                )
            }

            payload.isWelfarePreference() -> {
                JobWelfareSelectorActivity.start(
                    fragment.activity,
                    viewModel.sessionId,
                    viewModel.conversationId,
                )
            }

            payload.isBirthday() -> showBirthdayPicker()
            payload.isExpectedSalary() -> showSalaryPicker()
            payload.isWorkExperience() -> showWorkExperience(payload, questionQueueId)
            payload.isEducationInfo() -> showEducationExperience(payload, questionQueueId)
            payload.isProjectExperience() -> showProjectExperience(payload, questionQueueId)
            payload.isSkills() || payload.isCertificates() || payload.isAwards() -> {
                showCertificateSkillsAwards(payload, questionQueueId)
            }

            else -> return false
        }
        return true
    }

    private fun showBirthdayPicker() {
        BirthYearMonthPickerDialog.show(
            manager = fragment.childFragmentManager,
            maxTimestamp = 1767143497521,
            selectedYear = 2000,
            selectedMonth = 1,
            title = "出生年月",
            onConfirmWithTimestamp = { year, month, _ ->
                FlowEventBus.postSticky(Events.ClawAgentEvent("$year-$month"))
            }
        )
    }

    private fun showSalaryPicker() {
        SalaryRangePickerDialog.show(manager = fragment.childFragmentManager) { minSalary, maxSalary ->
            val payloadJson = SalaryRangePayload.fromPicker(minSalary, maxSalary).toJson()
            FlowEventBus.postSticky(
                Events.ClawAgentEvent(payloadJson, AppEvent.KLX_C_EXPECTED_SALARY_COMPLETED)
            )
        }
    }

    private fun showWorkExperience(
        payload: CoreQuestionPayloadBean,
        questionQueueId: String?,
    ) {
        WorkExperienceDialog.show(
            manager = fragment.childFragmentManager,
            questionQueueId = questionQueueId,
            attributeGroupCode = payload.attributeGroupCode,
            clientRequestId = viewModel.getOrCreateCurrentRequestId(),
            sessionId = viewModel.sessionId,
            conversationId = viewModel.conversationId,
            onCompleteClick = ::postQuestionCompleted,
        )
    }

    private fun showEducationExperience(
        payload: CoreQuestionPayloadBean,
        questionQueueId: String?,
    ) {
        EducationExperienceDialog.show(
            manager = fragment.childFragmentManager,
            questionQueueId = questionQueueId,
            attributeGroupCode = payload.attributeGroupCode,
            clientRequestId = viewModel.getOrCreateCurrentRequestId(),
            sessionId = viewModel.sessionId,
            conversationId = viewModel.conversationId,
            onCompleteClick = ::postQuestionCompleted,
        )
    }

    private fun showProjectExperience(
        payload: CoreQuestionPayloadBean,
        questionQueueId: String?,
    ) {
        ProjectExperienceDialog.show(
            manager = fragment.childFragmentManager,
            questionQueueId = questionQueueId,
            attributeGroupCode = payload.attributeGroupCode,
            clientRequestId = viewModel.getOrCreateCurrentRequestId(),
            sessionId = viewModel.sessionId,
            conversationId = viewModel.conversationId,
            onCompleteClick = ::postQuestionCompleted,
        )
    }

    private fun showCertificateSkillsAwards(
        payload: CoreQuestionPayloadBean,
        questionQueueId: String?,
    ) {
        CertificateSkillsAwardsDialog.show(
            manager = fragment.childFragmentManager,
            type = payload.attributeGroupCode.orEmpty(),
            questionQueueId = questionQueueId,
            attributeGroupCode = payload.attributeGroupCode,
            clientRequestId = viewModel.getOrCreateCurrentRequestId(),
            sessionId = viewModel.sessionId,
            conversationId = viewModel.conversationId,
            onCompleteClick = ::postQuestionCompleted,
        )
    }

    private fun postQuestionCompleted() {
        FlowEventBus.postSticky(Events.ClawAgentEvent(null))
    }
}
