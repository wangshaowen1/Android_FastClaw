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
import com.shiyuan.fastclaw.common.adapter.MultiTypeBindingAdapter
import com.shiyuan.fastclaw.common.adapter.MultiTypeProvider
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.databinding.FragmentHomeTreasuryBinding
import com.shiyuan.fastclaw.databinding.ItemHomeTreasuryCompanyBinding
import com.shiyuan.fastclaw.databinding.ItemHomeTreasuryMeasureBinding
import com.shiyuan.fastclaw.databinding.ItemHomeTreasuryPackageBinding
import com.shiyuan.fastclaw.databinding.ItemHomeTreasurySalaryBinding
import com.shiyuan.fastclaw.databinding.ItemHomeTreasuryStandardBinding
import com.shiyuan.fastclaw.feature.report.ItemHomeTreasuryCardVM
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.router.AppNavigator

class HomeTreasuryFragment : BaseViewModelFragment<FragmentHomeTreasuryBinding, HomeReportViewModel>() {

    private val treasuryAdapter = MultiTypeBindingAdapter<ItemHomeTreasuryCardVM>(
        emptyList(),
        object : MultiTypeProvider<ItemHomeTreasuryCardVM> {
            override fun getItemType(position: Int, item: ItemHomeTreasuryCardVM?): Int {
                return requireNotNull(item).itemType
            }

            override fun getLayoutId(viewType: Int): Int {
                return when (viewType) {
                    ItemHomeTreasuryCardVM.TYPE_COMPANY -> R.layout.item_home_treasury_company
                    ItemHomeTreasuryCardVM.TYPE_MEASURE -> R.layout.item_home_treasury_measure
                    ItemHomeTreasuryCardVM.TYPE_SALARY -> R.layout.item_home_treasury_salary
                    ItemHomeTreasuryCardVM.TYPE_INTERVIEW -> R.layout.item_home_treasury_standard
                    ItemHomeTreasuryCardVM.TYPE_PACKAGE -> R.layout.item_home_treasury_package
                    else -> error("Unsupported home treasury card type: $viewType")
                }
            }
        }
    ) { itemBinding, _, _ ->
        when (itemBinding) {
            is ItemHomeTreasuryCompanyBinding -> {
                itemBinding.tvDaily.text = buildHighlightText(
                    R.string.home_treasury_daily_free2,
                    "3",
                    "10"
                )
                itemBinding.root.setOnClickListener {
                    onCompanyCardClick()
                }
            }
            is ItemHomeTreasuryMeasureBinding -> {
                itemBinding.tvDaily.text = buildHighlightText(
                    R.string.home_treasury_daily_free3,
                    "0.99"
                )
                itemBinding.root.setOnClickListener {
                    onMeasureCardClick()
                }
            }
            is ItemHomeTreasurySalaryBinding -> {
                itemBinding.tvDaily.text = buildHighlightText(
                    R.string.home_treasury_daily_free3,
                    "0.99"
                )
                itemBinding.root.setOnClickListener {
                    onSalaryCardClick()
                }
            }
            is ItemHomeTreasuryStandardBinding -> {
                itemBinding.tvDaily.text = buildHighlightText(
                    R.string.home_treasury_shrimp_chip2,
                    "40"
                )
                itemBinding.root.setOnClickListener {
                    onInterviewCardClick()
                }
            }
            is ItemHomeTreasuryPackageBinding -> {
                itemBinding.root.setOnClickListener {
                    onPackageCardClick()
                }
            }

        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initTreasuryCards()
        observePassPackageResumeStatus()
    }

    private fun initTreasuryCards() {
        binding.rvTreasuryCards.adapter = treasuryAdapter
        treasuryAdapter.submitList(createTreasuryCards())
    }

    private fun createTreasuryCards(): List<ItemHomeTreasuryCardVM> {
        return listOf(
            ItemHomeTreasuryCardVM(ItemHomeTreasuryCardVM.TYPE_COMPANY),
            ItemHomeTreasuryCardVM(ItemHomeTreasuryCardVM.TYPE_MEASURE),
            ItemHomeTreasuryCardVM(ItemHomeTreasuryCardVM.TYPE_SALARY),
            ItemHomeTreasuryCardVM(ItemHomeTreasuryCardVM.TYPE_INTERVIEW),
            ItemHomeTreasuryCardVM(ItemHomeTreasuryCardVM.TYPE_PACKAGE))
    }

    private fun buildHighlightText(textRes: Int, vararg targets: String): CharSequence {
        val text = getString(textRes)
        val color = resources.getColor(R.color.color_2f6bff)
        return SpannableString(text).apply {
            targets.forEach { target ->
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
}
