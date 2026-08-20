package com.shiyuan.fastclaw.feature.opportunity

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.FragmentStateCacheAdapter
import com.shiyuan.fastclaw.databinding.ActivityProcessedOpportunityBinding
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel

/**
 * 已处理机会。
 */
class ProcessedOpportunityActivity :
    BaseViewModelActivity<ActivityProcessedOpportunityBinding, OpportunityViewModel>() {

    companion object {
        private const val EXTRA_SELECTED_INDEX = "extra_selected_index"
        private const val TAB_COMMUNICATING = 0
        private const val TAB_UNSUITABLE = 1

        fun start(activity: Activity?, selectedIndex: Int = TAB_COMMUNICATING) {
            activity ?: return
            activity.startActivity(
                Intent(activity, ProcessedOpportunityActivity::class.java).apply {
                    putExtra(EXTRA_SELECTED_INDEX, selectedIndex.coerceIn(0, 1))
                }
            )
        }
    }

    private val selectedIndex by lazy {
        intent.getIntExtra(EXTRA_SELECTED_INDEX, TAB_COMMUNICATING).coerceIn(0, 1)
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateTabState(position.coerceIn(0, 1))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_f6f9ff)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        updateTabCount(TAB_COMMUNICATING, 0)
        updateTabCount(TAB_UNSUITABLE, 0)
        binding.vpProcessedOpportunity.adapter =
            FragmentStateCacheAdapter(supportFragmentManager, lifecycle).apply {
                setFragments(
                    listOf(
                        ProcessedOpportunityListFragment.newInstance(TAB_COMMUNICATING),
                        ProcessedOpportunityListFragment.newInstance(TAB_UNSUITABLE)
                    )
                )
            }
        binding.vpProcessedOpportunity.offscreenPageLimit = 2
        binding.vpProcessedOpportunity.setCurrentItem(selectedIndex, false)
        updateTabState(selectedIndex)
        binding.llTabCommunicating.setOnSingleClickListener {
            binding.vpProcessedOpportunity.setCurrentItem(TAB_COMMUNICATING, false)
        }
        binding.llTabUnsuitable.setOnSingleClickListener {
            binding.vpProcessedOpportunity.setCurrentItem(TAB_UNSUITABLE, false)
        }
        binding.vpProcessedOpportunity.registerOnPageChangeCallback(pageChangeCallback)
    }

    internal fun updateTabCount(tabType: Int, count: Int) {
        when (tabType) {
            TAB_COMMUNICATING -> binding.tvTabCommunicating.text =
                getString(R.string.processed_opportunity_tab_communicating_count, count)

            TAB_UNSUITABLE -> binding.tvTabUnsuitable.text =
                getString(R.string.processed_opportunity_tab_unsuitable_count, count)
        }
    }

    private fun updateTabState(index: Int) {
        val communicatingSelected = index == TAB_COMMUNICATING
        binding.vTabCommunicatingIndicator.visibility =
            if (communicatingSelected) View.VISIBLE else View.INVISIBLE
        binding.vTabUnsuitableIndicator.visibility =
            if (communicatingSelected) View.INVISIBLE else View.VISIBLE
        binding.tvTabCommunicating.setTextColor(
            tabTextColor(
                if (communicatingSelected) R.color.color_90_000000 else R.color.color_55_3c3c43
            )
        )
        binding.tvTabUnsuitable.setTextColor(
            tabTextColor(
                if (communicatingSelected) R.color.color_55_3c3c43 else R.color.color_90_000000
            )
        )
        binding.tvTabCommunicating.typeface = typefaceForSelected(communicatingSelected)
        binding.tvTabUnsuitable.typeface = typefaceForSelected(!communicatingSelected)
    }

    private fun tabTextColor(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    private fun typefaceForSelected(selected: Boolean): Typeface =
        if (selected) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT

    override fun onDestroy() {
        binding.vpProcessedOpportunity.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }
}
