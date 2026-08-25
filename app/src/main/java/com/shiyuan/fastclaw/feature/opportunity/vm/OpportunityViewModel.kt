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

/** 机会首页的匹配展示状态。 */
enum class OpportunityMatchState {
    /** 尚未进入匹配。 */
    UNMATCHED,
    /** 正常匹配中，暂时没有新机会。 */
    NORMAL_MATCHING,
    /** 已匹配到新机会。 */
    MATCHED
}


/** 是否已满足开启机会匹配的前置条件。 */
enum class OpportunityIsCanMatchState {
    /** 简历或求职偏好尚未完善。 */
    UNMATCHED,
    /** 简历与求职偏好均已完善，可以开始匹配。 */
    CANMATCHED
}

/**
 * 机会模块共享 ViewModel。
 *
 * 负责简历与求职偏好完成状态、机会统计、新机会轮询以及已处理机会分页数据。
 */
class OpportunityViewModel : HomeReportViewModel() {

    /** 新机会轮询任务，同一时间只允许一个任务运行。 */
    private var newOpportunityPollingJob: Job? = null
    /** 简历解析状态轮询任务，同一时间只允许一个任务运行。 */
    private var resumePollingJob: Job? = null
    /** 按机会状态缓存分页流，避免 Tab 重建时重复创建 Pager。 */
    private val processedOpportunityPagingFlows =
        mutableMapOf<String, Flow<PagingData<OpportunityProcessedItemData>>>()

    /** 已处理机会页工具栏状态。 */
    val mToolbarVMProcessedOpportunity = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.not_opportunity_processed_title.getResString(),
        background = Color.TRANSPARENT
    )

    /** 最新简历是否解析成功。 */
    var isJobUpCompleted = MutableLiveData(false)
    /** 高重要度求职偏好是否已全部确认。 */
    var isJobPreferenceCompleted = MutableLiveData(false)


    /** 最近 5 条已处理机会，来自已处理机会分页接口。 */
    val chanceData = MutableLiveData<List<OpportunityProcessedItemData>>()

    /** 已处理机会分页响应，页面使用其中两个状态的总数。 */
    val processedOpportunityPageData = MutableLiveData<OpportunityProcessedPageData?>()

    /** 当前最新一批未处理机会。 */
    val matchingData = MutableLiveData<List<OpportunityNewItemData>>()

    /** 机会首页四项统计数据。 */
    val opportunityStats = MutableLiveData<OpportunityStatsData?>()


    /** 根据简历和求职偏好完成状态合并出的匹配资格。 */
    val isMatchState = MediatorLiveData<OpportunityIsCanMatchState>().apply {
        value = resolveIsCanMatchState()
        addSource(isJobUpCompleted) {
            value = resolveIsCanMatchState()
        }
        addSource(isJobPreferenceCompleted) {
            value = resolveIsCanMatchState()
        }
    }


    /** 退出登录或切换用户时停止任务并清空当前用户的机会数据。 */
    fun resetUserState() {
        stopNewOpportunityPolling()
        resumeListData.value = emptyList()
        isJobUpCompleted.value = false
        isJobPreferenceCompleted.value = false
        matchingData.value = emptyList()
        chanceData.value = emptyList()
        opportunityStats.value = null
    }

    /**
     * 查询常态化匹配卡片展示的四项统计数据。
     *
     * 请求：无业务参数。
     * 返回：[OpportunityStatsData]，成功后写入 [opportunityStats]。
     */
    fun queryOpportunityStats() {
        request(
            apiBlock = { mRepo.api.queryOpportunityStats() },
            onSuccess = {
                data?.let { opportunityStats.value = it }
            }
        )
    }

    /**
     * 查询首页最近 5 条已处理机会。
     *
     * 请求：第一页、每页 5 条，不传处理状态。
     * 返回：[OpportunityProcessedPageData]，页面只消费 opportunities.items。
     */
    fun queryProcessedOpportunityPreview() {
        request(
            apiBlock = { mRepo.api.queryProcessedOpportunityPage(PagedBody(1, 5)) },
            onSuccess = {
                chanceData.value = data?.opportunities?.items.orEmpty()
            }
        )
    }

    /**
     * 获取指定处理状态的 Paging3 数据流。
     *
     * 请求：[OpportunityProcessedPageBody]，页码和页大小由 Paging3 提供。
     * 返回：连续发射 [OpportunityProcessedItemData] 的分页流。
     *
     * @param status 处理状态，仅使用 COMMUNICATING 或 UNSUITABLE。
     */
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

    /**
     * 查询已处理机会两个 Tab 的实时总数。
     *
     * 请求：指定 [status] 的第一页，每页仅取 1 条以获取聚合数量。
     * 返回：[OpportunityProcessedPageData]，成功后写入 [processedOpportunityPageData]。
     */
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

    /**
     * 轮询最新简历的解析状态。
     *
     * 请求：第一页、最多 100 条简历。
     * 返回：简历列表；解析中持续轮询，进入成功或失败终态后写入 [resumeListData]。
     */
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

    /** 停止简历解析状态轮询。 */
    fun stopResumePolling() {
        resumePollingJob?.cancel()
        resumePollingJob = null
    }

    /**
     * 单次刷新最新机会。
     *
     * 请求：无业务参数。
     * 返回：[OpportunityNewItemData] 列表，成功后写入 [matchingData]。
     */
    fun queryNewOpportunities() {
        request(
            apiBlock = { mRepo.api.queryNewOpportunities() },
            onSuccess = {
                matchingData.value = data.orEmpty()
            }
        )
    }

    /**
     * 页面可见且处于常态化匹配时轮询查询新机会。
     *
     * 请求：每 5 秒查询一次，无业务参数。
     * 返回：新机会列表；列表非空时结束本轮轮询并写入 [matchingData]。
     */
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

    /** 停止新机会轮询。 */
    fun stopNewOpportunityPolling() {
        newOpportunityPollingJob?.cancel()
        newOpportunityPollingJob = null
    }

    /**
     * 查询高重要度求职属性是否已全部确认。
     *
     * 请求：无业务参数。
     * 返回：result 布尔值，成功后写入 [isJobPreferenceCompleted]。
     */
    fun queryJobPreferenceCompletion() {
        request(
            apiBlock = { mRepo.api.checkJobAgentAttributeAnswers() },
            onSuccess = {
                isJobPreferenceCompleted.value = data?.result == true
            }
        )
    }

    /** 仅当简历和求职偏好均完成时允许进入机会匹配。 */
    private fun resolveIsCanMatchState(): OpportunityIsCanMatchState {
        if (isJobUpCompleted.value == true && isJobPreferenceCompleted.value == true) {
            return OpportunityIsCanMatchState.CANMATCHED
        }else{
            return OpportunityIsCanMatchState.UNMATCHED
        }
    }

    /** ViewModel 销毁时取消所有长轮询任务。 */
    override fun onCleared() {
        stopResumePolling()
        stopNewOpportunityPolling()
        super.onCleared()
    }


    private companion object {
        const val PROCESSED_OPPORTUNITY_PAGE_SIZE = 20
    }
}
