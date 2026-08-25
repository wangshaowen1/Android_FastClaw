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

enum class OpportunityDetailActionState {
    NEW_OPPORTUNITY,
    COMMUNICATING,
    UNSUITABLE,
    CLOSED
}

/** 机会详情。 */
class OpportunityDetailViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVMOpportunityDetail = ToolbarViewModel(
        navigationAction = { finish() },
        background = Color.TRANSPARENT
    )

    val opportunityDetail = MutableLiveData<OpportunityDetailData>()

    /** 根据上一页传入的岗位 ID 查询机会详情。 */
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

    /** 提交详情页操作，完成后重新查询详情及最新可用操作。 */
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

    /** 创建企业侧 IM 会话，成功后交由页面进入聊天。 */
    fun createEnterpriseImSession(
        jobId: String,
        onSuccess: (ImSessionCreateData) -> Unit,
    ) {
        val targetBaseAccountId = User.get().userId?.toLongOrNull() ?: return
        val currentJobId = jobId.toLongOrNull() ?: return
        requestWithLoading(
            apiBlock = {
                mRepo.api.createEnterpriseImSession(
                    ImSessionCreateBody(
                        targetSubjectType = 1,
                        targetBaseAccountId = targetBaseAccountId,
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
