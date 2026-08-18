package com.shiyuan.fastclaw.feature.opportunity

import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.FragmentStateCacheAdapter
import com.shiyuan.fastclaw.databinding.FragmentHomeOpportunityBinding
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel
import com.shiyuan.fastclaw.feature.report.HomeTreasuryFragment

class HomeOpportunityFragment : BaseViewModelFragment<FragmentHomeOpportunityBinding, OpportunityViewModel>() {

    private val pagesAdapter by lazy {
        FragmentStateCacheAdapter(childFragmentManager, lifecycle).apply {
            setFragments(
                listOf(
                    HomeTreasuryFragment(),
                    NotOpportunityFragment(),
                )
            )
        }
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateTabState(position)
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initViewPager()
        initTabClickListeners()
        updateTabState(binding.vpOpportunityPages.currentItem)
    }

    private fun initViewPager() {
        binding.vpOpportunityPages.adapter = pagesAdapter
        binding.vpOpportunityPages.offscreenPageLimit = PAGE_COUNT
        binding.vpOpportunityPages.registerOnPageChangeCallback(pageChangeCallback)
    }

    private fun initTabClickListeners() {
        binding.tvTable1.setOnSingleClickListener {
            binding.vpOpportunityPages.setCurrentItem(OPPORTUNITY_PAGE, true)
        }
        binding.tvTable2.setOnSingleClickListener {
            binding.vpOpportunityPages.setCurrentItem(JOB_GUIDE_PAGE, true)
        }
    }

    private fun updateTabState(position: Int) {
        updateTabView(binding.tvTable1, position == OPPORTUNITY_PAGE)
        updateTabView(binding.tvTable2, position == JOB_GUIDE_PAGE)
    }

    private fun updateTabView(tabView: android.widget.TextView, selected: Boolean) {
        tabView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.color_000000 else R.color.color_75_3c3c43,
            )
        )
        tabView.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            if (selected) SELECTED_TEXT_SIZE_SP else UNSELECTED_TEXT_SIZE_SP,
        )
        tabView.typeface = if (selected) SELECTED_TYPEFACE else UNSELECTED_TYPEFACE
    }

    override fun onDestroyView() {
        binding.vpOpportunityPages.unregisterOnPageChangeCallback(pageChangeCallback)
        binding.vpOpportunityPages.adapter = null
        super.onDestroyView()
    }

    companion object {
        private const val OPPORTUNITY_PAGE = 0
        private const val JOB_GUIDE_PAGE = 1
        private const val PAGE_COUNT = 2
        private const val SELECTED_TEXT_SIZE_SP = 22f
        private const val UNSELECTED_TEXT_SIZE_SP = 18f

        private val SELECTED_TYPEFACE = Typeface.create("sans-serif", Typeface.BOLD)
        private val UNSELECTED_TYPEFACE = Typeface.create("sans-serif", Typeface.BOLD)
    }
}
