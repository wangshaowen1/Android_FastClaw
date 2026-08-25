package com.shiyuan.fastclaw.feature.opportunity.vm

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.ImSessionCreateBody
import com.shiyuan.fastclaw.core.network.apis.request.OpportunityDetailQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.OpportunityStatusUpdateBody
import com.shiyuan.fastclaw.core.network.apis.response.ImSessionCreateData
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityDetailData

/** 机会详情页底部操作区的展示状态。 */
enum class OpportunityDetailActionState {
    /** 新机会，可选择不合适或立即沟通。 */
    NEW_OPPORTUNITY,
    /** 已进入沟通，可继续沟通或标记不合适。 */
    COMMUNICATING,
    /** 已标记为不合适。 */
    UNSUITABLE,
    /** 岗位已关闭，操作不可用。 */
    CLOSED
}

/** 机会详情页 ViewModel，负责详情查询、状态更新和企业 IM 会话创建。 */
class OpportunityDetailViewModel : AppRepositoryViewModel<AppRepository>() {

    /** 详情页工具栏状态。 */
    val mToolbarVMOpportunityDetail = ToolbarViewModel(
        navigationAction = { finish() },
        background = Color.TRANSPARENT
    )

    /** 机会详情接口返回数据。 */
    val opportunityDetail = MutableLiveData<OpportunityDetailData>()

    /**
     * 根据上一页传入的岗位 ID 查询机会详情。
     *
     * 请求：[OpportunityDetailQueryBody]，包含 [jobId]。
     * 返回：[OpportunityDetailData]，成功后写入 [opportunityDetail]。
     */
    fun queryOpportunityDetail(jobId: String, showLoading: Boolean = true) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.queryOpportunityDetail(OpportunityDetailQueryBody(jobId))
            },
            showLoading = showLoading,
            onSuccess = {
                data?.let { detail ->
                    opportunityDetail.value = detail
                }
            }
        )
    }

    /**
     * 提交详情页操作，完成后重新查询详情及最新可用操作。
     *
     * 请求：[OpportunityStatusUpdateBody]，包含岗位 ID 和服务端下发的操作编码。
     * 返回：状态更新结果；页面不直接消费返回体，成功或业务失败时均重新查询详情。
     */
    fun updateOpportunityStatus(jobId: String, actionCode: String,onSuccess: () -> Unit,) {
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
                onSuccess()
            }
        )
    }

    /**
     * 创建企业侧 IM 会话，成功后交由页面进入聊天。
     *
     * 请求：[ImSessionCreateBody]，currentJobId 由 [jobId] 转为 Long。
     * 返回：[ImSessionCreateData]；仅在对端账号有效时回调 [onSuccess]。
     */
    fun createEnterpriseImSession(
        jobId: String,
        onSuccess: (ImSessionCreateData) -> Unit,
    ) {
        val currentJobId = jobId.toLongOrNull() ?: return
        requestWithLoading(
            apiBlock = {
                mRepo.api.createEnterpriseImSession(
                    ImSessionCreateBody(
                        currentJobId = currentJobId,
                    )
                )
            },
            onSuccess = {
                data?.takeIf { it.targetBaseAccountId > 0 }?.let(onSuccess)
            }
        )
    }
}
