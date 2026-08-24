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

/** 公司主页。 */
class CompanyDetailViewModel : AppRepositoryViewModel<AppRepository>() {

    val toolbar = ToolbarViewModel(
        navigationDrawable = R.drawable.ic_company_detail_back.getDrawable(),
        navigationAction = { finish() },
        title = R.string.opportunity_company_detail_title.getResString(),
        titleColor = Color.WHITE,
        background = Color.TRANSPARENT,
    )

    val companyDetail = MutableLiveData<OpportunityCompanyDetailData>()
    val companyName = MutableLiveData<String>()
    val companySummary = MutableLiveData<String>()

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
