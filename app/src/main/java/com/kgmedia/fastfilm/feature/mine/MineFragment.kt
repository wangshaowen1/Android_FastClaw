package com.kgmedia.fastfilm.feature.mine

import android.content.Context
import android.text.style.AbsoluteSizeSpan
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentPagerAdapter
import com.blankj.utilcode.util.ClipboardUtils
import com.drake.spannable.addSpan
import com.kgmedia.fastfilm.BuildConfig
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.adapter.FragmentsAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentMineBinding
import com.kgmedia.fastfilm.feature.main.MainViewModel
import com.kgmedia.fastfilm.feature.mine.chase.BingeWatchFragment
import com.kgmedia.fastfilm.feature.mine.drafts.UserShortDramaDraftsFragment
import com.kgmedia.fastfilm.feature.mine.episode.EpisodeFragment
import com.kgmedia.fastfilm.feature.mine.like.LikeFragment
import com.kgmedia.fastfilm.feature.payment.points.PointsRechargeActivity
import com.kgmedia.fastfilm.feature.payment.vip.MembershipBenefitsActivity
import com.kgmedia.fastfilm.feature.revenue.CreateRevenueActivity
import com.kgmedia.fastfilm.feature.settings.SettingsActivity
import com.kgmedia.fastfilm.feature.settings.personal.PersonalInformationActivity
import com.kgmedia.fastfilm.router.AppNavigator
import com.google.android.material.appbar.AppBarLayout
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.showToast
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:08
 * @Version: 1.0
 * *****************************************
 */
class MineFragment : BaseViewModelFragment<FragmentMineBinding, MineViewModel>() {

    companion object {
        private val INDEX_EPISODE = 0
        private val INDEX_SHORT = 1
        private val INDEX_BINGE = 2
        private val INDEX_LIKE = 3
    }

    private val hostVm by lazy { getViewModelOfActivity<MainViewModel>() }
    private var userId: String? = null

    // 添加阈值参数，控制开始改变alpha的滑动百分比（0.0-1.0）
    // 滑动到50%时开始改变alpha
    private val alphaChangeThreshold = 0.5f

    private val titles = arrayListOf<CharSequence>()

    private val fragmentList by lazy {
        listOf(
            //小说
            EpisodeFragment.newInstance(),
            //草稿
            UserShortDramaDraftsFragment.newInstance(),
            //追更
            BingeWatchFragment.newInstance(),
            //喜欢
            LikeFragment.newInstance(),
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        //更新必要信息
        mRealVM.updateInformation()
    }

    override fun onResume() {
        super.onResume()
        //更新必要信息
        mRealVM.updateInformation()
    }

    private fun initView() {
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            // 获取总滚动范围
            val ratio = calculateAlphaRatio(appBarLayout, verticalOffset)
            binding.sbv.alpha = ratio
            binding.viewMenu.alpha = ratio
        }

        //初始化tabs
        initTabs()
        //更新标题
        updateTitle()
        binding.vp.adapter = FragmentsAdapter(
            fragments = fragmentList,
            fm = childFragmentManager,
            behavior = FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )
    }

    private inner class TitleAdapter : CommonNavigatorAdapter() {

        // 用来缓存 View，方便后续取出
        private val titleViews = SparseArray<IPagerTitleView>()

        override fun getCount(): Int {
            return titles.size
        }

        override fun getTitleView(context: Context, index: Int): IPagerTitleView {
            val titleView = ColorTransitionPagerTitleView(context)
            titleView.gravity = Gravity.CENTER
            titleView.setIsSelectedBold(true)
            titleView.setPadding(12.dp, 0, 12.dp, 0)
            titleView.text = titles[index]
            titleView.textSize = 16f // MagicIndicator 单位通常默认是 sp，如果需要 dp 需转换
            titleView.normalColor = R.color.color_50_121212.getResColor()
            titleView.selectedColor = R.color.color_121212.getResColor()

            titleView.setOnClickListener { binding.vp.currentItem = index }

            // 关键：存入缓存
            titleViews.put(index, titleView)
            return titleView
        }

        override fun getIndicator(context: Context): IPagerIndicator? {
            return null
        }

        fun updateTitleAt(index: Int, newContent: CharSequence) {
            if (index < titles.size) {
                titles[index] = newContent
            }

            val view = titleViews.get(index)
            if (view is ColorTransitionPagerTitleView) {
                view.text = newContent
            } else if (view is TextView) {
                view.text = newContent
            }
        }
    }

    private val myNavigatorAdapter by lazy { TitleAdapter() }

    /**
     *初始化tabs
     */
    private fun initTabs() {
        val activity = activity ?: return
        val navigator = CommonNavigator(activity)
        navigator.isAdjustMode = true
        navigator.isReselectWhenLayout = true
        navigator.adapter = myNavigatorAdapter
        binding.magicIndicator.navigator = navigator

        ViewPagerHelper.bind(binding.magicIndicator, binding.vp)
    }

    /**
     * 更新标题
     */
    private fun updateTitle() {
        titles.clear()
        titles.add(setTitleContent(mRealVM.episode.value ?: 0, "作品"))
        titles.add(setTitleContent(mRealVM.shortCount.value ?: 0, "草稿"))
        titles.add(setTitleContent(mRealVM.bingeWatch.value ?: 0, "追更"))
        titles.add(setTitleContent(mRealVM.like.value ?: 0, "喜欢"))
        myNavigatorAdapter.notifyDataSetChanged()
    }

    private fun setTitleContent(size: Int, text: String): CharSequence {
        return if (size < 1) text else text.addSpan(
            "$size",
            AbsoluteSizeSpan(10, true)
        )
    }

    /**
     * 计算透明度比例
     */
    private fun calculateAlphaRatio(appBarLayout: AppBarLayout, verticalOffset: Int): Float {
        val totalScrollRange = appBarLayout.totalScrollRange
        if (totalScrollRange == 0) return 0f

        val rawRatio = abs(verticalOffset).toFloat() / totalScrollRange

        // 添加阈值判断：只有当滑动比例超过阈值时才开始计算alpha变化
        return if (rawRatio < alphaChangeThreshold) {
            0f // 低于阈值时完全不透明（alpha为0）
        } else {
            // 高于阈值时，重新映射比例到0-1范围
            val mappedRatio = (rawRatio - alphaChangeThreshold) / (1f - alphaChangeThreshold)
            max(0f, min(1f, mappedRatio))
        }
    }


    private fun dataUpdate() {
        mRealVM.noLogIn.observe(this) { it ->
            if (!it) return@observe
            if ((hostVm?.showTab?.get() in setOf(3, 4))) {
                hostVm?.startCreating?.value = true
            }
        }

        mRealVM.episode.observe(this) { count ->
            updateSingleTab(INDEX_EPISODE, count ?: 0, "作品")
        }

        mRealVM.shortCount.observe(this) { count ->
            updateSingleTab(INDEX_SHORT, count ?: 0, "草稿")
        }

        mRealVM.bingeWatch.observe(this) { count ->
            updateSingleTab(INDEX_BINGE, count ?: 0, "追更")
        }

        mRealVM.like.observe(this) { count ->
            updateSingleTab(INDEX_LIKE, count ?: 0, "喜欢")
        }
        observeEvents {
            /**
             * 登录状态变化
             */
            onUserStatusChanged {
                mRealVM.refreshUserInfo()
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id
                if (User.get().isVisitor && isShowReception()) {
                    hostVm?.startCreating?.value = true
                }
            }

            /**
             * 用户信息变化
             */
            on<Events.UserDataEvent> {
                mRealVM.refreshUserInfo()
            }

            on<Events.MainTabDoubleClickEvent> { event ->
                if (event.tabType == Events.MainTabDoubleClickEvent.TAB_MINE) {
                    if (isActive) {
                        binding.appbar.setExpanded(true)
                    }
                }
            }
        }
    }

    // 封装一个简单的辅助函数
    private fun updateSingleTab(index: Int, count: Int, baseText: String) {
        val content = setTitleContent(count, baseText)
        myNavigatorAdapter.updateTitleAt(index, content)
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.siv_avatar,
            R.id.tv_nickname -> {
                //个人资料
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                PersonalInformationActivity.start(requireActivity())
            }

            R.id.siv_mine_settings -> {
                //设置
                if (AppNavigator.checkLoginIntercepted(false) && !BuildConfig.DEBUG) {
                    return
                }
                SettingsActivity.start(requireActivity())
            }

            R.id.tv_id, R.id.iv_copy -> {
                //复制
                User.get().appId?.let {
                    ClipboardUtils.copyText(it)
                    getString(R.string.toast_copied).showToast()
                }
            }

            R.id.ll_points -> {
                //积分
                PointsRechargeActivity.start(topActivity, false)
            }

            R.id.iv_vip_bg -> {
                EventManager.onAppTrack(
                    AppEvent.JG_CLICK_MY_PAGE_VIP_ENTRY
                )
                MembershipBenefitsActivity.start(topActivity, withToast = false)
            }

            R.id.iv_mine_create_revenue -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                //创造收益
                CreateRevenueActivity.start(activity)
            }
        }
    }

}