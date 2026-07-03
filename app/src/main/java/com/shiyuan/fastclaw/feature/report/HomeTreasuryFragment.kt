package com.shiyuan.fastclaw.feature.report

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.databinding.FragmentHomeTreasuryBinding
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.router.AppNavigator

class HomeTreasuryFragment : BaseViewModelFragment<FragmentHomeTreasuryBinding, HomeReportViewModel>() {

    override fun onPrepareView() {
        super.onPrepareView()
        setDailyText()
        observePassPackageResumeStatus()
    }

    private fun setDailyText() {
        val text = getString(R.string.home_treasury_daily_free2)
        val color = resources.getColor(R.color.color_2f6bff)
        binding.tvDaily.text = SpannableString(text).apply {
            listOf("3", "10").forEach { target ->
                val start = text.indexOf(target)
                if (start >= 0) {
                    setSpan(
                        ForegroundColorSpan(color),
                        start,
                        start + target.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }

    private fun observePassPackageResumeStatus() {
        mRealVM.interviewPassPackageResumeStatus.observe(this) { data ->
            val url = if (data?.resumeCheckId == null) {
                Constants.PASS_PACKAGE_RESUME_UPLOAD_URL
            } else {
                "${Constants.PASS_PACKAGE_JOB_UPLOAD_URL}?resumeCheckId=${data?.resumeCheckId}"
            }
            SimpleBrowserActivity.launch(requireActivity(), url)
        }
    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.card_company,
            R.id.ll_company_action -> {
                if (!AppNavigator.checkLoginIntercepted(true)) {
                    FlowEventBus.postSync(Events.CompanyInterviewEvent(0))
                    FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
                }
            }

            R.id.card_interview,
            R.id.ll_interview_action  -> {
                if (!AppNavigator.checkLoginIntercepted(true)) {
                    FlowEventBus.postSync(Events.CompanyInterviewEvent(1))
                    FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
                }
            }
            R.id.card_package,
            R.id.ll_package_action  -> {
                if (!AppNavigator.checkLoginIntercepted(true)) {
                    mRealVM.queryInterviewPassPackageResumeStatus()
                }
            }
            R.id.ll_resume_check_tool-> {
                if (!AppNavigator.checkLoginIntercepted(true)) {
                    SimpleBrowserActivity.launch(requireActivity(), Constants.DIAGNOSIS_URL, title = getString(R.string.home_treasury_resume_check))
                }
            }
            R.id.ll_salary_tool ->  {
                if (!AppNavigator.checkLoginIntercepted(true)) {
                    SimpleBrowserActivity.launch(requireActivity(), Constants.SALARY_URL, title = getString(R.string.home_treasury_salary_check))
                }
            }
        }
    }
}
