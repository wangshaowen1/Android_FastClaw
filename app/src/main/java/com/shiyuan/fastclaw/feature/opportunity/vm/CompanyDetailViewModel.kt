package com.shiyuan.fastclaw.feature.opportunity.vm

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.OpportunityCompanyDetailQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityCompanyDetailData

/** 公司主页 ViewModel，负责查询并整理机会关联公司的公开信息。 */
class CompanyDetailViewModel : AppRepositoryViewModel<AppRepository>() {

    /** 公司主页工具栏状态。 */
    val toolbar = ToolbarViewModel(
        navigationDrawable = R.drawable.ic_company_detail_back.getDrawable(),
        navigationAction = { finish() },
        title = R.string.opportunity_company_detail_title.getResString(),
        titleColor = Color.WHITE,
        background = Color.TRANSPARENT,
    )

    /** 公司详情接口的完整返回数据。 */
    val companyDetail = MutableLiveData<OpportunityCompanyDetailData>()
    /** 页面优先展示企业名称，为空时回退到品牌名称。 */
    val companyName = MutableLiveData<String>()
    /** 由行业、规模和融资阶段组合成的公司摘要。 */
    val companySummary = MutableLiveData<String>()

    /**
     * 查询机会关联公司的公开信息。
     *
     * 请求：[OpportunityCompanyDetailQueryBody]，包含企业业务 ID。
     * 返回：[OpportunityCompanyDetailData]，成功后同步详情、公司名称和摘要。
     */
    fun queryCompanyDetail(enterpriseId: Long) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.queryOpportunityCompanyDetail(
                    OpportunityCompanyDetailQueryBody(enterpriseId)
                )
            },
            onSuccess = {
                data?.let { detail ->
                    companyDetail.value = detail
                    companyName.value = detail.enterpriseName.ifBlank { detail.brandName }
                    companySummary.value = listOf(
                        detail.industryName,
                        detail.scaleName,
                        detail.financingStageName,
                    ).filter(String::isNotBlank).joinToString("·")
                }
            }
        )
    }
}
