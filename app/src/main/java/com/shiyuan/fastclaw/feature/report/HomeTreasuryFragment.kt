package com.shiyuan.fastclaw.feature.report

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.databinding.FragmentHomeTreasuryBinding
import com.shiyuan.fastclaw.databinding.ItemHomeTreasuryBinding
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.router.AppNavigator

class HomeTreasuryFragment : BaseViewModelFragment<FragmentHomeTreasuryBinding, HomeReportViewModel>() {

    private val treasuryAdapter =
        SingleBindingAdapter<ItemHomeTreasuryBinding, JobAgentModuleData>(
            R.layout.item_home_treasury,
            emptyList()
        ) { itemBinding, _, item ->
            itemBinding.root.setBackgroundResource(item.localBackgroundRes())
            itemBinding.ivIcon.setImageResource(item.localIconRes())
            itemBinding.tvTitle.text = item.title
            itemBinding.tvFree.visibility = if (item.free == true) View.VISIBLE else View.GONE
            itemBinding.tvDescription.text = item.description
            itemBinding.tvBenefit.text = buildBenefitText(item.benefit.orEmpty())
            itemBinding.root.setOnClickListener {
                when (item.reportType) {
                    JobAgentModuleData.TYPE_COMPANY_RESEARCH -> onCompanyCardClick()
                    JobAgentModuleData.TYPE_RESUME_REJECTION_ANALYSIS -> onMeasureCardClick()
                    JobAgentModuleData.TYPE_SALARY_VALUATION -> onSalaryCardClick()
                    JobAgentModuleData.TYPE_MOCK_INTERVIEW -> onInterviewCardClick()
                    JobAgentModuleData.TYPE_INTERVIEW_PASS_PACKAGE_REPORT -> onPackageCardClick()
                }
            }
        }

    override fun onPrepareView() {
        super.onPrepareView()
        initTreasuryCards()
        observeJobAgentModuleList()
        observePassPackageResumeStatus()
        mRealVM.queryJobAgentModuleList()
    }

    private fun initTreasuryCards() {
        binding.rvTreasuryCards.adapter = treasuryAdapter
    }

    private fun observeJobAgentModuleList() {
        mRealVM.jobAgentModuleList.observe(this) { modules ->
            treasuryAdapter.submitList(modules.filter { module -> module.isSupported() })
        }
    }

    private fun JobAgentModuleData.isSupported(): Boolean {
        return reportType in SUPPORTED_REPORT_TYPES
    }

    private fun JobAgentModuleData.localIconRes(): Int {
        return when (reportType) {
            JobAgentModuleData.TYPE_COMPANY_RESEARCH -> R.drawable.img_home_treasury_company
            JobAgentModuleData.TYPE_RESUME_REJECTION_ANALYSIS -> R.drawable.img_home_treasury_measure
            JobAgentModuleData.TYPE_SALARY_VALUATION -> R.drawable.img_home_treasury_salary
            JobAgentModuleData.TYPE_MOCK_INTERVIEW -> R.drawable.img_home_treasury_interview
            JobAgentModuleData.TYPE_INTERVIEW_PASS_PACKAGE_REPORT -> R.drawable.img_home_treasury_package
            else -> 0
        }
    }

    private fun JobAgentModuleData.localBackgroundRes(): Int {
        return when (reportType) {
            JobAgentModuleData.TYPE_MOCK_INTERVIEW,
            JobAgentModuleData.TYPE_INTERVIEW_PASS_PACKAGE_REPORT -> R.drawable.bg_home_treasury_card2
            else -> R.drawable.bg_home_treasury_card
        }
    }

    private fun buildBenefitText(text: String): CharSequence {
        val color = resources.getColor(R.color.color_2f6bff)
        return SpannableString(text).apply {
            BENEFIT_NUMBER_REGEX.findAll(text).forEach { match ->
                setSpan(
                    ForegroundColorSpan(color),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun onCompanyCardClick() {
        if (AppNavigator.checkLoginIntercepted(true)) return
        FlowEventBus.postSync(Events.CompanyInterviewEvent(0))
        FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
    }

    private fun onMeasureCardClick() {
        if (AppNavigator.checkLoginIntercepted(true)) return
        SimpleBrowserActivity.launch(
            requireActivity(),
            Constants.DIAGNOSIS_URL,
            title = getString(R.string.home_treasury_resume_check)
        )
   /*     SimpleBrowserActivity.launch(
            context,
            "file:///android_asset/wechat-return-test.html"
        )*/
    }

    private fun onSalaryCardClick() {
        if (AppNavigator.checkLoginIntercepted(true)) return
        SimpleBrowserActivity.launch(
            requireActivity(),
            Constants.SALARY_URL,
            title = getString(R.string.home_treasury_salary_check)
        )
    }

    private fun onInterviewCardClick() {
        if (AppNavigator.checkLoginIntercepted(true)) return
        FlowEventBus.postSync(Events.CompanyInterviewEvent(1))
        FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
    }

    private fun onPackageCardClick() {
        if (AppNavigator.checkLoginIntercepted(true)) return
        mRealVM.queryInterviewPassPackageResumeStatus()
    }

    private fun observePassPackageResumeStatus() {
        mRealVM.interviewPassPackageResumeStatus.observe(this) { data ->
            val url = if (data?.resumeCheckId == null) {
                Constants.PASS_PACKAGE_INDEX_URL
            } else {
                "${Constants.PASS_PACKAGE_INDEX_URL}&resumeCheckId=${data?.resumeCheckId}"
            }
            SimpleBrowserActivity.launch(requireActivity(), url)
        }
    }

    companion object {
        private val BENEFIT_NUMBER_REGEX = Regex("""\d+(?:\.\d+)?""")
        private val SUPPORTED_REPORT_TYPES = setOf(
            JobAgentModuleData.TYPE_COMPANY_RESEARCH,
            JobAgentModuleData.TYPE_RESUME_REJECTION_ANALYSIS,
            JobAgentModuleData.TYPE_SALARY_VALUATION,
            JobAgentModuleData.TYPE_MOCK_INTERVIEW,
            JobAgentModuleData.TYPE_INTERVIEW_PASS_PACKAGE_REPORT,
        )
    }
}
