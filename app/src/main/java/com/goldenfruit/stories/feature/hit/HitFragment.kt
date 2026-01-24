package com.kgmedia.fastfilm.feature.hit

import android.view.Gravity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.utils.CommonNavigatorUtils
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.model.ShortDramasStatisticsItem
import com.kgmedia.fastfilm.databinding.FragmentHitBinding
import com.kgmedia.fastfilm.feature.creator.CreationRootActivity
import com.kgmedia.fastfilm.feature.hit.recommend.HitRecommendFragment
import com.kgmedia.fastfilm.feature.hit.tutorial.TutorialChildFragment
import com.kgmedia.fastfilm.router.AppNavigator
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.crop.util.ViewUtils
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ScaleTransitionPagerTitleView

class HitFragment :
    BaseViewModelFragment<FragmentHitBinding, HitViewModel>() {
    companion object {
        fun newInstance(): HitFragment {
            val fragment = HitFragment()
            return fragment
        }
    }

    private val releaseDataAdapter by lazy {
        QuickAdapter(R.layout.item_release_data, arrayListOf())
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
        binding.btnCreation.setOnSingleClickListener {
            if (AppNavigator.checkLoginIntercepted()) return@setOnSingleClickListener
            EventManager.onAppTrack(AppEvent.JG_CLICK_FADDISH_PAGE_CREATE_NOW_BUTTON)
            CreationRootActivity.start(activity)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            binding.container.resume() // Fragment 显示时恢复动画
        } else {
            binding.container.pause() // Fragment 隐藏时暂停动画
        }
    }

    override fun onResume() {
        super.onResume()
        binding.container.resume() // Fragment 可见时恢复动画
    }

    override fun onPause() {
        super.onPause()
        binding.container.pause() // Fragment 不可见时暂停动画
    }

    private fun initView() {
        binding.llTop.minimumHeight = ViewUtils.getStatusBarHeight(activity)
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange.toFloat()
            val ratio = kotlin.math.abs(verticalOffset) / totalScrollRange
            val alpha = if (ratio < 0.95f) 1f - ratio / 0.95f else 0f
            binding.llTop.alpha = alpha
        }
        binding.rvReleaseData.adapter = releaseDataAdapter
        binding.magicIndicator.navigator = CommonNavigatorUtils.getCommonNavigator(
            mActivity,
            listOf(
                "爆款推荐",
                "攻略资讯"
            ),
            binding.viewPager,
            indicatorView = CommonNavigatorUtils.getLinePagerIndicatorGradient(context = mActivity)
                .also { indicator ->
                    indicator?.lineHeight = (6).dp.toFloat()
                    indicator?.lineWidth = (46).dp.toFloat()
                    indicator?.roundRadius = (6).dp.toFloat()
                    indicator?.mode = LinePagerIndicator.MODE_EXACTLY
                    indicator?.yOffset = (4).dp.toFloat()
                },
            titleViewCreator = { context, index, title ->
                val titleView = ScaleTransitionPagerTitleView(context)
                titleView.setIsSelectedBold(true)
                titleView.setPadding(14.dp, 0, 14.dp, 0)
                titleView.setLines(1)
                titleView.textSize = 18f
                titleView.gravity = Gravity.CENTER
                titleView.text = title
                titleView.normalColor = R.color.color_50_121212.getResColor()
                titleView.selectedColor = R.color.color_121212.getResColor()
                titleView.setOnClickListener { binding.viewPager.setCurrentItem(index, true) }
                titleView
            },
            isBold = false
        )
        binding.viewPager.adapter = object : FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                if (position == 0) {
                    return HitRecommendFragment.newInstance()
                }
                return TutorialChildFragment.newInstance()
            }

            override fun getCount(): Int {
                return 2
            }
        }
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager)
        binding.viewPager.setCurrentItem(0, false)

        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position > 0) {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_FADDISH_PAGE_CONSULT_STRATEGY_CATEGORY_BUTTON)
                }
            }
        })
    }

    fun dataUpdate() {
        mRealVM.statisticsData.observe(this) {
            releaseDataAdapter.submitList(
                listOf(
                    ItemReleaseDataVm(ShortDramasStatisticsItem("累计入驻", it.totalEnroll.toString(), "人")),
                    ItemReleaseDataVm(ShortDramasStatisticsItem("累计创作", it.totalCreation.toString(), "章")),
                    ItemReleaseDataVm(ShortDramasStatisticsItem("小说市场规模", it.shortDramasMarketValue.toString(), "亿"))
                )
            )
        }
        mRealVM.danmakuList.observe(this) {
//            // 初始化列表
//            val list = List(10) {
//                DanmakuItem(
//                    avatarUrl = "https://randomuser.me/api/portraits/men/${it}.jpg",
//                    tipContext = if (it % 2 == 0) "用户$it：弹幕测试～" else "用户$it：长弹幕测试～*************开始看"
//                )
//            }
            if (!it.isNullOrEmpty()) {
                // 启动弹幕
                binding.container.start(it)
            }
        }
    }
}