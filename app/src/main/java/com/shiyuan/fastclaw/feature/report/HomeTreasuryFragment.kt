package com.shiyuan.fastclaw.feature.report

import android.view.View
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.FragmentHomeTreasuryBinding
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import com.shiyuan.fastclaw.router.AppNavigator

class HomeTreasuryFragment : BaseViewModelFragment<FragmentHomeTreasuryBinding, HomeReportViewModel>() {

    override fun onPrepareView() {
        super.onPrepareView()
    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.card_company,
            R.id.ll_company_action -> {
                if (!AppNavigator.checkLoginIntercepted(true)) {

                }
            }

            R.id.card_interview,
            R.id.ll_interview_action  -> {
                if (!AppNavigator.checkLoginIntercepted(true)) {

                }
            }
            R.id.ll_resume_check_tool-> {
                if (!AppNavigator.checkLoginIntercepted(true)) {

                }
            }
            R.id.ll_salary_tool ->  {
                if (!AppNavigator.checkLoginIntercepted(true)) {

                }
            }
        }
    }
}
