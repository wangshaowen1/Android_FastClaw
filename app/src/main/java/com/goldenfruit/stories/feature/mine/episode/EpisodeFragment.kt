package com.kgmedia.fastfilm.feature.mine.episode

import android.view.View
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.adapter.FragmentsAdapter
import com.kgmedia.fastfilm.databinding.FragmentEpisodeBinding
import com.kgmedia.fastfilm.feature.mine.MineViewModel
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfParentFragment

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:小说
 * @Data: 2025/11/4/周二  17:18
 * @Version: 1.0
 * *****************************************
 */
class EpisodeFragment : BaseViewModelFragment<FragmentEpisodeBinding, EpisodeViewModel>() {

    companion object {

        fun newInstance(): EpisodeFragment {
            val fragment = EpisodeFragment()
            return fragment
        }
    }

    private val fragmentList by lazy {
        listOf(
            EpisodeListFragment.newInstance(EpisodeListFragment.LISTING_TYPE_LAUNCH),
            EpisodeListFragment.newInstance(EpisodeListFragment.LISTING_TYPE_NOT_LAUNCH)
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()

        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {}

            override fun onPageSelected(position: Int) {
                mRealVM.isLaunch.set(position == 0)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        binding.viewPager.adapter = FragmentsAdapter(
            fragments = fragmentList,
            fm = childFragmentManager,
            behavior = FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )
    }

    private fun dataUpdate() {
        mRealVM.totalCount.observe(viewLifecycleOwner) {
            getViewModelOfParentFragment<MineViewModel>()?.apply {
                episode.value =
                    (mRealVM.totalCount.value ?: 0) + (mRealVM.totalCountNotLaunch.value ?: 0)
            }
        }
        mRealVM.totalCountNotLaunch.observe(viewLifecycleOwner) {
            getViewModelOfParentFragment<MineViewModel>()?.apply {
                episode.value =
                    (mRealVM.totalCount.value ?: 0) + (mRealVM.totalCountNotLaunch.value ?: 0)
            }
        }

    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.tv_listed -> {
                mRealVM.isLaunch.set(true)
                binding.viewPager.currentItem = 0
            }

            R.id.tv_not_listed -> {
                mRealVM.isLaunch.set(false)
                binding.viewPager.currentItem = 1
            }
        }
    }
}