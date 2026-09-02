package com.shiyuan.fastclaw.feature.opportunity

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
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_INTERVIEW_PASS
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_LOGOUT
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_RESUME_TEST
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_SALARY_TEST
import com.shiyuan.fastclaw.core.manager.AppEventParam
import com.shiyuan.fastclaw.core.manager.AppEventParam.BAODIAN_TAB
import com.shiyuan.fastclaw.core.manager.AppEventParam.BAODIAN_TAB_COMPANY_CHECK
import com.shiyuan.fastclaw.core.manager.AppEventParam.BAODIAN_TAB_INTERVIEW_PASS
import com.shiyuan.fastclaw.core.manager.AppEventParam.BAODIAN_TAB_MOCK_INTERVIEW
import com.shiyuan.fastclaw.core.manager.AppEventParam.BAODIAN_TAB_RESUME_TEST
import com.shiyuan.fastclaw.core.manager.AppEventParam.BAODIAN_TAB_SALARY_TEST
import com.shiyuan.fastclaw.core.manager.AppEventParam.KLX_ENTRY_SOURCE
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.databinding.FragmentHomeTreasuryBinding
import com.shiyuan.fastclaw.databinding.ItemHomeTreasuryBinding
import com.shiyuan.fastclaw.feature.report.HomeReportViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.router.AppNavigator

/**
 * 求职宝典工具列表页。
 *
 * 请求服务端配置的求职工具模块，只展示当前客户端支持的类型，并按类型分发入口。
 */
class HomeTreasuryFragment : BaseViewModelFragment<FragmentHomeTreasuryBinding, HomeReportViewModel>() {

    /** 服务端模块数据与本地图标、点击入口的绑定适配器。 */
    private val treasuryAdapter =
        SingleBindingAdapter<ItemHomeTreasuryBinding, JobAgentModuleData>(
            R.layout.item_home_treasury,
            emptyList()
        ) { itemBinding, _, item ->
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
        loadTreasuryCards()
    }

    private fun initTreasuryCards() {
        binding.rvTreasuryCards.adapter = treasuryAdapter
        binding.lsv.setRetryListener { loadTreasuryCards() }
    }

    /**
     * 请求求职宝典模块列表。
     *
     * 请求：无业务参数。
     * 返回：JobAgentModuleData 列表，由 [observeJobAgentModuleList] 过滤并展示。
     */
    private fun loadTreasuryCards() {
        binding.lsv.showLoading()
        mRealVM.queryJobAgentModuleList { error ->
            if (view != null) {
                binding.lsv.showRetry(error)
            }
        }
    }

    /** 消费模块列表返回数据，并切换内容、空态或重试状态。 */
    private fun observeJobAgentModuleList() {
        mRealVM.jobAgentModuleList.observe(viewLifecycleOwner) { modules ->
            val supportedModules = modules.filter { module -> module.isSupported() }
            treasuryAdapter.submitList(supportedModules)
            if (supportedModules.isEmpty()) {
                binding.lsv.showEmpty()
            } else {
                binding.lsv.showContent()
            }
        }
    }

    /** 仅保留当前客户端已实现点击入口的模块类型。 */
    private fun JobAgentModuleData.isSupported(): Boolean {
        return reportType in SUPPORTED_REPORT_TYPES
    }

    /** 将服务端模块类型映射到当前客户端内置图标。 */
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


    /** 高亮权益文案中的数字部分。 */
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

    /** 公司查询入口：切换到首页的企业查询页面。 */
    private fun onCompanyCardClick() {
        if (AppNavigator.checkLoginIntercepted(false,loginSource = BAODIAN_TAB_COMPANY_CHECK)) return
        FlowEventBus.postSync(Events.CompanyInterviewEvent(0))
        FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
    }

    /** 简历测评入口：完成登录拦截与埋点后打开 H5。 */
    private fun onMeasureCardClick() {
        if (AppNavigator.checkLoginIntercepted(false,loginSource = BAODIAN_TAB_RESUME_TEST)) return
        EventManager.onAppTrack(KLX_CLICK_RESUME_TEST,mutableMapOf(KLX_ENTRY_SOURCE to BAODIAN_TAB))
        SimpleBrowserActivity.launch(
            requireActivity(),
            Constants.DIAGNOSIS_URL/*,
            title = getString(R.string.home_treasury_resume_check)*/
        )
    }

    /** 薪资测评入口：完成登录拦截与埋点后打开 H5。 */
    private fun onSalaryCardClick() {
        if (AppNavigator.checkLoginIntercepted(false,loginSource = BAODIAN_TAB_SALARY_TEST)) return
        EventManager.onAppTrack(KLX_CLICK_SALARY_TEST,mutableMapOf(KLX_ENTRY_SOURCE to BAODIAN_TAB))
        SimpleBrowserActivity.launch(
            requireActivity(),
            Constants.SALARY_URL,
           /* title = getString(R.string.home_treasury_salary_check)*/
        )
    }

    /** 模拟面试入口：切换到首页的模拟面试页面。 */
    private fun onInterviewCardClick() {
        if (AppNavigator.checkLoginIntercepted(false,loginSource = BAODIAN_TAB_MOCK_INTERVIEW)) return
        FlowEventBus.postSync(Events.CompanyInterviewEvent(1))
        FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_HOME))
    }

    /**
     * 面试直通车入口。
     *
     * 请求简历检测状态，响应结果由 [observePassPackageResumeStatus] 处理。
     */
    private fun onPackageCardClick() {
        if (AppNavigator.checkLoginIntercepted(false,loginSource = BAODIAN_TAB_INTERVIEW_PASS)) return
        EventManager.onAppTrack(KLX_CLICK_INTERVIEW_PASS,mutableMapOf(KLX_ENTRY_SOURCE to BAODIAN_TAB))
        mRealVM.queryInterviewPassPackageResumeStatus()
    }

    /** 根据返回的 resumeCheckId 拼接面试直通车地址并打开 H5。 */
    private fun observePassPackageResumeStatus() {
        mRealVM.interviewPassPackageResumeStatus.observe(this) { data ->
            val url = if (data?.resumeCheckId == null) {
                Constants.PASS_PACKAGE_INDEX_URL
            } else {
                "${Constants.PASS_PACKAGE_INDEX_URL}&resumeCheckId=${data.resumeCheckId}"
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
