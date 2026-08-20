package com.shiyuan.fastclaw.feature.opportunity.vm

import android.graphics.Color
import androidx.databinding.ObservableField
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.InterviewPassPackageResumeStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeListDeleteBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitMyBody
import com.shiyuan.fastclaw.core.network.apis.response.InterviewPassPackageResumeStatusData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeInfoSection
import com.shiyuan.fastclaw.core.network.apis.response.ResumeListData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitMyData
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.opportunity.ProcessedOpportunityItem
import com.shiyuan.fastclaw.feature.report.data.ReportRepository
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import java.util.UUID

enum class OpportunityMatchState {
    UNMATCHED,
    NORMAL_MATCHING,
    MATCHED
}

/**
 * @Introduce:
 */
class OpportunityViewModel : HomeReportViewModel() {

    val mToolbarVMOpportunityDetail = ToolbarViewModel(
        navigationAction = { finish() },
        background = Color.TRANSPARENT
    )

    val opportunityDetail = ObservableField(OpportunityDetailUiModel.preview())

    val mToolbarVMProcessedOpportunity = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.not_opportunity_processed_title.getResString(),
        background = Color.TRANSPARENT
    )

    var isJobUpCompleted = MutableLiveData(false)
    var isJobPreferenceCompleted = MutableLiveData(false) // TODO 岗位偏好接口接入后使用接口结果


    val chanceData = MutableLiveData<List<ProcessedOpportunityItem>>(emptyList())

    val matchingData = MutableLiveData<List<ProcessedOpportunityItem>>(emptyList())


    val matchState = MediatorLiveData<OpportunityMatchState>().apply {
        value = resolveMatchState()
        addSource(isJobUpCompleted) {
            value = resolveMatchState()
        }
        addSource(isJobPreferenceCompleted) {
            value = resolveMatchState()
        }
        addSource(chanceData) {
            value = resolveMatchState()
        }
    }


    fun resetUserState() {
        resumeListData.value = emptyList()
        isJobUpCompleted.value = false
        isJobPreferenceCompleted.value = false
    }

    private fun resolveMatchState(): OpportunityMatchState {
        if (isJobUpCompleted.value != true || isJobPreferenceCompleted.value != true) {
            return OpportunityMatchState.UNMATCHED
        }
        return if (chanceData.value.orEmpty().isEmpty()) {
            OpportunityMatchState.NORMAL_MATCHING
        } else {
            OpportunityMatchState.MATCHED
        }
    }

     fun createFakeChanceData(): List<ProcessedOpportunityItem> = listOf(
        ProcessedOpportunityItem(
            title = R.string.processed_opportunity_sample_high_backend.getResString(),
            company = R.string.processed_opportunity_sample_company.getResString(),
            requirements = R.string.processed_opportunity_sample_requirements.getResString(),
            salary = R.string.processed_opportunity_sample_salary.getResString(),
            actionTime = R.string.processed_opportunity_sample_action_time.getResString(),
            matchPercent = 76,
            isRead = false
        ),
        ProcessedOpportunityItem(
            title = R.string.processed_opportunity_sample_backend.getResString(),
            company = R.string.processed_opportunity_sample_company.getResString(),
            requirements = R.string.processed_opportunity_sample_requirements.getResString(),
            salary = R.string.processed_opportunity_sample_salary.getResString(),
            actionTime = R.string.processed_opportunity_sample_action_time.getResString(),
            matchPercent = 76,
            isRead = true
        )
    )
}

data class OpportunityDetailUiModel(
    val title: String,
    val company: String,
    val location: String,
    val experience: String,
    val education: String,
    val salary: String,
    val matchPercent: Int,
    val introductionMarkdown: String,
    val descriptionHtml: String,
    val companyProfile: String,
    val address: String,
    val securityTip: String
) {
    companion object {
        fun preview() = OpportunityDetailUiModel(
            title = R.string.opportunity_detail_sample_title.getResString(),
            company = R.string.opportunity_detail_sample_company.getResString(),
            location = R.string.opportunity_detail_sample_location.getResString(),
            experience = R.string.opportunity_detail_sample_experience.getResString(),
            education = R.string.opportunity_detail_sample_education.getResString(),
            salary = R.string.opportunity_detail_sample_salary.getResString(),
            matchPercent = 76,
            introductionMarkdown = R.string.opportunity_detail_sample_introduction_markdown.getResString(),
            descriptionHtml = R.string.opportunity_detail_sample_description_html.getResString(),
            companyProfile = R.string.opportunity_detail_sample_company_profile.getResString(),
            address = R.string.opportunity_detail_sample_address.getResString(),
            securityTip = R.string.opportunity_detail_security_tip_content.getResString()
        )
    }
}
