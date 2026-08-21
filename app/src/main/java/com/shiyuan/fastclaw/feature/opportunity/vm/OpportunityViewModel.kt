package com.shiyuan.fastclaw.feature.opportunity.vm

import android.graphics.Color
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.PollingState
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchPolling
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.InterviewPassPackageResumeStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.OpportunityDetailQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.OpportunityProcessedPageBody
import com.shiyuan.fastclaw.core.network.apis.request.OpportunityStatusUpdateBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeListDeleteBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitMyBody
import com.shiyuan.fastclaw.core.network.apis.response.InterviewPassPackageResumeStatusData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeInfoSection
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityDetailActionData
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityDetailData
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityNewItemData
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityProcessedItemData
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityProcessedPageData
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityStatsData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeListData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitMyData
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.opportunity.ProcessedOpportunityPagingSource
import com.shiyuan.fastclaw.feature.report.data.ReportRepository
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import java.util.UUID

enum class OpportunityMatchState {
    UNMATCHED,
    NORMAL_MATCHING,
    MATCHED
}

enum class OpportunityDetailActionState {
    NEW_OPPORTUNITY,
    COMMUNICATING,
    UNSUITABLE,
    CLOSED
}

/**
 * @Introduce:
 */
class OpportunityViewModel : HomeReportViewModel() {

    private var newOpportunityPollingJob: Job? = null
    private var resumePollingJob: Job? = null
    private val processedOpportunityPagingFlows =
        mutableMapOf<String, Flow<PagingData<OpportunityProcessedItemData>>>()

    val mToolbarVMOpportunityDetail = ToolbarViewModel(
        navigationAction = { finish() },
        background = Color.TRANSPARENT
    )

    val opportunityDetail = MutableLiveData<OpportunityDetailData>()

    val mToolbarVMProcessedOpportunity = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.not_opportunity_processed_title.getResString(),
        background = Color.TRANSPARENT
    )

    var isJobUpCompleted = MutableLiveData(false)
    var isJobPreferenceCompleted = MutableLiveData(false) // TODO 岗位偏好接口接入后使用接口结果


    val chanceData = MutableLiveData<List<OpportunityProcessedItemData>>(emptyList())

    val processedOpportunityPageData = MutableLiveData<OpportunityProcessedPageData?>()

    val matchingData = MutableLiveData<List<OpportunityNewItemData>>(emptyList())

    val opportunityStats = MutableLiveData<OpportunityStatsData?>()


    val matchState = MediatorLiveData<OpportunityMatchState>().apply {
        value = resolveMatchState()
        addSource(isJobUpCompleted) {
            value = resolveMatchState()
        }
        addSource(isJobPreferenceCompleted) {
            value = resolveMatchState()
        }
        addSource(matchingData) {
            value = resolveMatchState()
        }
    }


    fun resetUserState() {
        stopNewOpportunityPolling()
        resumeListData.value = emptyList()
        isJobUpCompleted.value = false
        isJobPreferenceCompleted.value = false
        matchingData.value = emptyList()
        chanceData.value = emptyList()
        opportunityStats.value = null
    }

    /** 查询常态化匹配卡片展示的四项统计数据。 */
    fun queryOpportunityStats() {
        request(
            apiBlock = { mRepo.api.queryOpportunityStats() },
            onSuccess = {
                data?.let { opportunityStats.value = it }
            }
        )
    }

    /** 查询首页最近 5 条已处理机会，不传 status。 */
    fun queryProcessedOpportunityPreview() {
        request(
            apiBlock = { mRepo.api.queryProcessedOpportunityPage(PagedBody(1, 5)) },
            onError = {
                chanceData.value = createFakeChanceData()
            },
            onSuccess = {
                chanceData.value = data?.opportunities?.items.orEmpty()
            }
        )
    }

    fun processedOpportunityPagingFlow(
        status: String
    ): Flow<PagingData<OpportunityProcessedItemData>> {
        return processedOpportunityPagingFlows.getOrPut(status) {
            Pager(
                config = PagingConfig(
                    pageSize = PROCESSED_OPPORTUNITY_PAGE_SIZE,
                    initialLoadSize = PROCESSED_OPPORTUNITY_PAGE_SIZE,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = {
                    ProcessedOpportunityPagingSource(mRepo.api, status)
                }
            ).flow.cachedIn(viewModelScope)
        }
    }

    /** 查询已处理机会两个 Tab 的实时总数。 */
    fun queryProcessedOpportunityCounts(status: String) {
        request(
            apiBlock = {
                mRepo.api.queryProcessedOpportunityPage(
                    OpportunityProcessedPageBody(
                        status = status,
                        pageNum = 1,
                        pageSize = 1
                    )
                )
            },
            onSuccess = {
                processedOpportunityPageData.value = data
            }
        )
    }

    fun startResumePolling() {
        if (User.get().isVisitor || resumePollingJob?.isActive == true) return

        resumePollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true,
            showToastError = false,
            apiBlock = { mRepo.api.resumeQuery(PagedBody(1, 100)) },
            checkState = { response ->
                if (response?.isSuccess != true) {
                    PollingState.CONTINUE
                } else {
                    val latestResume = response.data?.items.orEmpty()
                        .maxByOrNull { it.uploadTime.orEmpty() }
                    if (latestResume?.parseStatus == 0 || latestResume?.parseStatus == 1) {
                        PollingState.CONTINUE
                    } else {
                        PollingState.SUCCESS
                    }
                }
            },
            onSuccess = { response ->
                resumePollingJob = null
                resumeListData.value = response?.data?.items.orEmpty()
            },
            onPolling = { response ->
                if (response?.isSuccess == true) {
                    resumeListData.value = response.data?.items.orEmpty()
                }
            }
        )
    }

    fun stopResumePolling() {
        resumePollingJob?.cancel()
        resumePollingJob = null
    }

    /** 页面可见且处于常态化匹配时，轮询查询新机会。 */
    fun startNewOpportunityPolling() {
        if (newOpportunityPollingJob?.isActive == true) return

        newOpportunityPollingJob = launchPolling(
            intervalMs = 5000L,
            retryOnNetworkError = false,
            showToastError = false,
            apiBlock = { mRepo.api.queryNewOpportunities() },
            checkState = { response ->
                when {
                    response?.isSuccess == true &&  response.data.orEmpty().isNotEmpty() -> PollingState.SUCCESS
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                newOpportunityPollingJob = null
                matchingData.value = response?.data.orEmpty()
            }
        )
    }

    fun stopNewOpportunityPolling() {
        newOpportunityPollingJob?.cancel()
        newOpportunityPollingJob = null
    }

    /** 根据上一页传入的岗位 ID 查询机会详情。 */
    fun queryOpportunityDetail(jobId: String, showLoading: Boolean = true) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.queryOpportunityDetail(OpportunityDetailQueryBody(jobId))
            },
            showLoading = showLoading,
            onError = {
                opportunityDetail.value = createFakeOpportunityDetail(jobId)
            },
            onSuccess = {
                data?.let { detail ->
                    opportunityDetail.value = detail
                }
            }
        )
    }

    /** 提交详情页操作，完成后重新查询详情及最新可用操作。 */
    fun updateOpportunityStatus(jobId: String, actionCode: String) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.updateOpportunityStatus(
                    OpportunityStatusUpdateBody(jobId, actionCode)
                )
            },
            onError = {
                queryOpportunityDetail(jobId, showLoading = false)
            },
            onSuccess = {
                queryOpportunityDetail(jobId, showLoading = false)
            }
        )
    }

    private fun createFakeOpportunityDetail(jobId: String) = OpportunityDetailData(
        jobId = jobId,
        jobTitle = R.string.opportunity_detail_sample_title.getResString(),
        matchPercent = 76,
        status = "NEW",
       /* jobClosed = true,*/
        companyName = R.string.opportunity_detail_sample_company.getResString(),
        salaryText = R.string.opportunity_detail_sample_salary.getResString(),
        workCity = R.string.opportunity_detail_sample_location.getResString(),
        experienceText = R.string.opportunity_detail_sample_experience.getResString(),
        degreeText = R.string.opportunity_detail_sample_education.getResString(),
        recommendationReason =
            R.string.opportunity_detail_sample_introduction_markdown.getResString(),
        jobDescription = R.string.opportunity_detail_sample_description_markdown.getResString(),
        companyIndustry = "互联网",
        companyScale = "10000人以上",
        financingStage = "已上市",
        workAddress = R.string.opportunity_detail_sample_address.getResString(),
        actions = listOf(
            OpportunityDetailActionData(
                buttonText = R.string.opportunity_detail_unsuitable.getResString(),
                actionCode = "MARK_UNSUITABLE"
            ),
            OpportunityDetailActionData(
                buttonText = R.string.opportunity_detail_communicate_now.getResString(),
                actionCode = "MARK_COMMUNICATING"
            )
        )
    )

    private fun resolveMatchState(): OpportunityMatchState {
        if (isJobUpCompleted.value != true || isJobPreferenceCompleted.value != true) {
            return OpportunityMatchState.UNMATCHED
        }
        return if (matchingData.value.orEmpty().isEmpty()) {
            OpportunityMatchState.NORMAL_MATCHING
        } else {
            OpportunityMatchState.MATCHED
        }
    }

    override fun onCleared() {
        stopResumePolling()
        stopNewOpportunityPolling()
        super.onCleared()
    }

     fun createFakeChanceData(): List<OpportunityProcessedItemData> = listOf(
        OpportunityProcessedItemData(
            jobTitle = R.string.processed_opportunity_sample_high_backend.getResString(),
            companyName = R.string.processed_opportunity_sample_company.getResString(),
            salaryText = R.string.processed_opportunity_sample_salary.getResString(),
            workCity = R.string.opportunity_detail_sample_location.getResString(),
            experienceText = R.string.opportunity_detail_sample_experience.getResString(),
            degreeText = R.string.opportunity_detail_sample_education.getResString(),
            matchPercent = 76,
            status = OpportunityProcessedItemData.STATUS_UNSUITABLE,
            statusTime = "2026-08-05 16:45:00"
        ),
        OpportunityProcessedItemData(
            jobTitle = R.string.processed_opportunity_sample_backend.getResString(),
            companyName = R.string.processed_opportunity_sample_company.getResString(),
            salaryText = R.string.processed_opportunity_sample_salary.getResString(),
            workCity = R.string.opportunity_detail_sample_location.getResString(),
            experienceText = R.string.opportunity_detail_sample_experience.getResString(),
            degreeText = R.string.opportunity_detail_sample_education.getResString(),
            matchPercent = 76,
            status = OpportunityProcessedItemData.STATUS_COMMUNICATING,
            statusTime = "2026-08-05 16:45:00"
        ),
        OpportunityProcessedItemData(
            jobTitle = R.string.processed_opportunity_sample_backend.getResString(),
            companyName = R.string.processed_opportunity_sample_company.getResString(),
            salaryText = R.string.processed_opportunity_sample_salary.getResString(),
            workCity = R.string.opportunity_detail_sample_location.getResString(),
            experienceText = R.string.opportunity_detail_sample_experience.getResString(),
            degreeText = R.string.opportunity_detail_sample_education.getResString(),
            matchPercent = 76,
            status = OpportunityProcessedItemData.STATUS_COMMUNICATING,
            statusTime = "2026-08-05 16:45:00"
        ),
        OpportunityProcessedItemData(
            jobTitle = R.string.processed_opportunity_sample_backend.getResString(),
            companyName = R.string.processed_opportunity_sample_company.getResString(),
            salaryText = R.string.processed_opportunity_sample_salary.getResString(),
            workCity = R.string.opportunity_detail_sample_location.getResString(),
            experienceText = R.string.opportunity_detail_sample_experience.getResString(),
            degreeText = R.string.opportunity_detail_sample_education.getResString(),
            matchPercent = 76,
            status = OpportunityProcessedItemData.STATUS_COMMUNICATING,
            statusTime = "2026-08-05 16:45:00"
        ),
        OpportunityProcessedItemData(
            jobTitle = R.string.processed_opportunity_sample_backend.getResString(),
            companyName = R.string.processed_opportunity_sample_company.getResString(),
            salaryText = R.string.processed_opportunity_sample_salary.getResString(),
            workCity = R.string.opportunity_detail_sample_location.getResString(),
            experienceText = R.string.opportunity_detail_sample_experience.getResString(),
            degreeText = R.string.opportunity_detail_sample_education.getResString(),
            matchPercent = 76,
            status = OpportunityProcessedItemData.STATUS_COMMUNICATING,
            statusTime = "2026-08-05 16:45:00"
        )
    )

    private companion object {
        const val PROCESSED_OPPORTUNITY_PAGE_SIZE = 20
    }
}
