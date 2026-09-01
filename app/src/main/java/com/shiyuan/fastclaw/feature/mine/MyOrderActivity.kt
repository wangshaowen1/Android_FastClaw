package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.FragmentStateCacheAdapter
import com.shiyuan.fastclaw.databinding.ActivityMyOrderBinding
import com.shiyuan.fastclaw.feature.mine.fragment.MyOrderRecordFragment
import com.shiyuan.fastclaw.feature.mine.fragment.MyShrimpChipsFragment
import com.shiyuan.fastclaw.feature.mine.viewModel.MineViewModel

/**
 * @Introduce: 我的订单
 */
class MyOrderActivity : BaseViewModelActivity<ActivityMyOrderBinding, MineViewModel>() {

    companion object {
        private const val EXTRA_SELECTED_INDEX = "extra_selected_index"

        fun start(activity: Activity?, selectedIndex: Int = 0) {
            activity ?: return
            val intent = Intent(activity, MyOrderActivity::class.java).apply {
                putExtra(EXTRA_SELECTED_INDEX, selectedIndex.coerceIn(0, 1))
            }
            activity.startActivity(intent)
        }
    }

    private val selectedIndex: Int by lazy {
        intent.getIntExtra(EXTRA_SELECTED_INDEX, 0).coerceIn(0, 1)
    }
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            updateTabSlider(position, positionOffset)
        }

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
            .navigationBarColor(R.color.color_f5f6fa)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.vpOrderPages.adapter =
            FragmentStateCacheAdapter(supportFragmentManager, lifecycle).apply {
                setFragments(
                    listOf(
                        MyShrimpChipsFragment.newInstance(),
                        MyOrderRecordFragment.newInstance()
                    )
                )
            }
        binding.vpOrderPages.offscreenPageLimit = 2
        binding.vpOrderPages.setCurrentItem(selectedIndex, false)
        updateTabState(selectedIndex)
        binding.layoutOrderTab.post { updateTabSlider(selectedIndex, 0f) }
        binding.llTabShrimpRecord.setOnSingleClickListener {
            binding.vpOrderPages.setCurrentItem(0, false)
            updateTabSlider(0, 0f)
        }
        binding.llTabOrderRecord.setOnSingleClickListener {
            binding.vpOrderPages.setCurrentItem(1, false)
            updateTabSlider(1, 0f)
        }
        binding.vpOrderPages.registerOnPageChangeCallback(pageChangeCallback)
    }

    private fun updateTabState(index: Int) {
        val shrimpSelected = index == 0
        binding.tvTabShrimpRecord.setTextColor(
            tabTextColor(if (shrimpSelected) R.color.white else R.color.color_75_3c3c43)
        )
        binding.tvTabOrderRecord.setTextColor(
            tabTextColor(if (shrimpSelected) R.color.color_75_3c3c43 else R.color.white)
        )
        binding.tvTabShrimpRecord.typeface = typefaceForSelected(shrimpSelected)
        binding.tvTabOrderRecord.typeface = typefaceForSelected(!shrimpSelected)
    }

    private fun updateTabSlider(position: Int, positionOffset: Float) {
        val startTab = if (position.coerceIn(0, 1) == 0) {
            binding.llTabShrimpRecord
        } else {
            binding.llTabOrderRecord
        }
        val endTab = if (position <= 0) binding.llTabOrderRecord else startTab
        val slider = binding.vOrderTabSlider
        val startX = startTab.left + (startTab.width - slider.width) / 2f - slider.left
        val endX = endTab.left + (endTab.width - slider.width) / 2f - slider.left
        slider.translationX = startX + (endX - startX) * positionOffset.coerceIn(0f, 1f)
    }

    private fun tabTextColor(colorRes: Int): Int {
        return ContextCompat.getColor(this, colorRes)
    }

    private fun typefaceForSelected(selected: Boolean) =
        if (selected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

    override fun onDestroy() {
        binding.vpOrderPages.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }
}
