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
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.InterviewPassPackageResumeStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.OpportunityProcessedPageBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeListDeleteBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitMyBody
import com.shiyuan.fastclaw.core.network.apis.response.InterviewPassPackageResumeStatusData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeInfoSection
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


enum class OpportunityIsCanMatchState {
    UNMATCHED,
    CANMATCHED
}

/**
 * @Introduce:
 */
class OpportunityViewModel : HomeReportViewModel() {

    private var newOpportunityPollingJob: Job? = null
    private var resumePollingJob: Job? = null
    private val processedOpportunityPagingFlows =
        mutableMapOf<String, Flow<PagingData<OpportunityProcessedItemData>>>()

    val mToolbarVMProcessedOpportunity = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.not_opportunity_processed_title.getResString(),
        background = Color.TRANSPARENT
    )

    var isJobUpCompleted = MutableLiveData(false)
    var isJobPreferenceCompleted = MutableLiveData(false)


    val chanceData = MutableLiveData<List<OpportunityProcessedItemData>>()

    val processedOpportunityPageData = MutableLiveData<OpportunityProcessedPageData?>()

    val matchingData = MutableLiveData<List<OpportunityNewItemData>>()

    val opportunityStats = MutableLiveData<OpportunityStatsData?>()


    val isMatchState = MediatorLiveData<OpportunityIsCanMatchState>().apply {
        value = resolveIsCanMatchState()
        addSource(isJobUpCompleted) {
            value = resolveIsCanMatchState()
        }
        addSource(isJobPreferenceCompleted) {
            value = resolveIsCanMatchState()
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

    /** 单次刷新最新机会。 */
    fun queryNewOpportunities() {
        request(
            apiBlock = { mRepo.api.queryNewOpportunities() },
            onSuccess = {
                matchingData.value = data.orEmpty()
            }
        )
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

    /** 查询高重要度求职属性是否已全部确认。 */
    fun queryJobPreferenceCompletion() {
        request(
            apiBlock = { mRepo.api.checkJobAgentAttributeAnswers() },
            onSuccess = {
                isJobPreferenceCompleted.value = data?.result == true
            }
        )
    }

    private fun resolveIsCanMatchState(): OpportunityIsCanMatchState {
        if (isJobUpCompleted.value == true && isJobPreferenceCompleted.value == true) {
            return OpportunityIsCanMatchState.CANMATCHED
        }else{
            return OpportunityIsCanMatchState.UNMATCHED
        }
    }

    override fun onCleared() {
        stopResumePolling()
        stopNewOpportunityPolling()
        super.onCleared()
    }


    private companion object {
        const val PROCESSED_OPPORTUNITY_PAGE_SIZE = 20
    }
}
