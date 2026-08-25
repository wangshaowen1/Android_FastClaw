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

/**
 * 机会模块顶部容器。
 *
 * 固定承载“机会”和“求职宝典”两个页面，并维护 Tab 与 ViewPager2 的双向同步。
 */
class HomeOpportunityFragment : BaseViewModelFragment<FragmentHomeOpportunityBinding, OpportunityViewModel>() {

    /** 子 Fragment 只创建一次，由缓存适配器维护生命周期。 */
    private val pagesAdapter by lazy {
        FragmentStateCacheAdapter(childFragmentManager, lifecycle).apply {
            setFragments(
                listOf(
                    OpportunityFragment(),
                    HomeTreasuryFragment(),
                )
            )
        }
    }

    /** 横向滑动后同步顶部 Tab 状态。 */
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

    /** 绑定固定页面并注册滑动监听。 */
    private fun initViewPager() {
        binding.vpOpportunityPages.adapter = pagesAdapter
        binding.vpOpportunityPages.offscreenPageLimit = PAGE_COUNT
        binding.vpOpportunityPages.registerOnPageChangeCallback(pageChangeCallback)
    }

    /** 点击 Tab 时切换到对应页面。 */
    private fun initTabClickListeners() {
        binding.tvTable1.setOnSingleClickListener {
            binding.vpOpportunityPages.setCurrentItem(OPPORTUNITY_PAGE, true)
        }
        binding.tvTable2.setOnSingleClickListener {
            binding.vpOpportunityPages.setCurrentItem(JOB_GUIDE_PAGE, true)
        }
    }

    /** 根据当前页同步两个 Tab 的选中态。 */
    private fun updateTabState(position: Int) {
        updateTabView(binding.tvTable1, position == OPPORTUNITY_PAGE)
        updateTabView(binding.tvTable2, position == JOB_GUIDE_PAGE)
    }

    /** 更新单个 Tab 的颜色、字号和字重。 */
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
        // View 销毁时解除 ViewPager2 引用和回调，避免持有旧视图。
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
