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
import com.shiyuan.fastclaw.feature.report.data.ReportRepository
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import java.util.UUID

/**
 * @Introduce:
 */
class OpportunityViewModel : HomeReportViewModel() {

    var isJobUpCompleted = MutableLiveData(false)
    var isJobPreferenceCompleted = MutableLiveData(false) // TODO 岗位偏好接口接入后使用接口结果

    var isCompleted = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(isJobUpCompleted) {
            value = isJobPreferenceCompleted.value == true && it == true
        }
        addSource(isJobPreferenceCompleted) {
            value = it == true && isJobUpCompleted.value == true
        }
    }

    fun resetUserState() {
        resumeListData.value = emptyList()
        isJobUpCompleted.value = false
        isJobPreferenceCompleted.value = false
    }
}
