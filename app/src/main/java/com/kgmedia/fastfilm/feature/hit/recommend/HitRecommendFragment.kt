package com.kgmedia.fastfilm.feature.hit.recommend

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.network.apis.entity.HitTabInfo
import com.kgmedia.fastfilm.databinding.FragmentHitRecommendBinding
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.WrapPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView

class HitRecommendFragment : BaseViewModelFragment<FragmentHitRecommendBinding, HitRecommendViewModel>() {
    companion object {
        fun newInstance(): HitRecommendFragment {
            val fragment = HitRecommendFragment()
            return fragment
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        mRealVM.tabs.observe(this) {
            it?.let {
                binding.loadStateViewHolder.isVisible = false
                initTabs(it)
            }
        }
        binding.loadStateViewHolder.setRetryListener {
            mRealVM.refreshTabs()
        }
    }

    fun dataUpdate() {

    }

    private fun initTabs(tabs: List<HitTabInfo>) {
        val fragments = tabs.map { tab -> HitRecommendChildFragment.newInstance(tab) }
        binding.viewPager.adapter = object : FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return tabs.size
            }
        }

        val navigator = CommonNavigator(activity)
        navigator.isAdjustMode = false
        navigator.scrollPivotX = 0.25f
        navigator.isReselectWhenLayout = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int {
                return tabs.size
            }

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val titleView = SimplePagerTitleView(context)
                titleView.setIsSelectedBold(true)
                titleView.setPadding(12.dp, 0, 12.dp, 0)
                titleView.setLines(1)
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                titleView.gravity = Gravity.CENTER
                titleView.text = tabs.getOrNull(index)?.tabName
                titleView.normalColor = R.color.textColorPrimary.getResColor()
                titleView.selectedColor = R.color.white.getResColor()
                titleView.setSelectedBackground(R.drawable.gradient_left_ff4332_right_ffa029_r50)
                titleView.setNormalBackground(R.drawable.bg_20_c8c8c8_r50)
                titleView.setOnClickListener {
                    binding.viewPager.setCurrentItem(index, false)
                }
                return titleView
            }

            override fun getIndicator(context: Context) = WrapPagerIndicator(context).apply {
                roundRadius = 5f.dp.toFloat()
            }
        }
        binding.magicIndicator.navigator = navigator
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager)
        setupTitleContainerMargins(navigator.titleContainer)
        binding.viewPager.setCurrentItem(0, false)
    }

    private fun setupTitleContainerMargins(container: ViewGroup) {
        container.children.forEachIndexed { index, view ->
            (view.layoutParams as? MarginLayoutParams)?.apply {
                if (index == 0) leftMargin = 16.dp
                rightMargin = 20.dp
            }
        }
    }
}