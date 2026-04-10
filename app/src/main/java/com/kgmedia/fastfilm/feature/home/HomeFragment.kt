package com.kgmedia.fastfilm.feature.home

import android.view.View
import android.widget.ImageView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.SPUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.databinding.loadGifUrl
import com.kgmedia.fastfilm.core.databinding.loadUri
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.core.network.apis.response.ActivityConfigData
import com.kgmedia.fastfilm.core.network.apis.response.FloatWindowInfo
import com.kgmedia.fastfilm.core.network.apis.response.PopupInfo
import com.kgmedia.fastfilm.core.network.apis.response.SimpleActivityInfo
import com.kgmedia.fastfilm.databinding.FragmentHomeBinding
import com.kgmedia.fastfilm.feature.creator.CreationRootActivity
import com.kgmedia.fastfilm.feature.main.ActivityDialog
import com.kgmedia.fastfilm.feature.main.MainViewModel
import com.kgmedia.fastfilm.feature.recharge.GuideAddCustomerServiceDialog
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.decoration.StaggeredGridSpaceItemDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.utils.Logger
import com.nhsd.crop.util.ViewUtils
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment :
    BaseViewModelFragment<FragmentHomeBinding, HomeViewModel>() {
    companion object {
        private const val KEY_VISIT_COUNT = "visit_count_home_fragment"
        fun newInstance(): HomeFragment {
            val fragment = HomeFragment()
            return fragment
        }
    }

    private val homeAdapter by lazy {
        HomeAdapter()
    }
    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>
    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.llTop.minimumHeight = ViewUtils.getStatusBarHeight(activity)
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange.toFloat()
            val ratio = abs(verticalOffset) / totalScrollRange
            val alpha = if (ratio < 0.95f) 1f - ratio / 0.95f else 0f
            binding.llTop.alpha = alpha
        }
        bannerAdapter = object : BaseBannerAdapter<BannerInfo>() {
            override fun bindData(
                holder: BaseViewHolder<BannerInfo>?, data: BannerInfo, position: Int, pageSize: Int
            ) {
                holder?.apply {
                    val image = findViewById<ImageView>(R.id.image)
                    loadUri(image, data.imageUrl, R.drawable.img_default_cover_16_9.getResDrawable())
                    image.setOnSingleClickListener {
                        AppRouter.jump(data.jumpUri)
                    }
                }
            }

            override fun getLayoutId(viewType: Int): Int {
                return R.layout.item_message_banner
            }
        }
        binding.banner.setAdapter(bannerAdapter)
            .setLifecycleRegistry(lifecycle)
            .setAutoPlay(true)
            .setIndicatorSliderRadius(2f.dp)
            .setIndicatorSliderGap(4.dp)
            .setIndicatorHeight(4.dp)
            .setIndicatorSliderWidth(4.dp, 16.dp)
            .setIndicatorSliderColor(
                R.color.color_60_ffffff.getResColor(), R.color.white.getResColor()
            ).setIndicatorStyle(IndicatorStyle.ROUND_RECT)
            .setIndicatorMargin(0, 0, 0, 4.dp)
            .setIndicatorView(binding.ivIndicator)
            .setIndicatorVisibility(View.VISIBLE)
            .create()

        binding.recyclerView.apply {
            isFocusable = false
            addItemDecoration(StaggeredGridSpaceItemDecoration(2, 10.dp, 0, 0))
            adapter = homeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = homeAdapter,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    displayOnEmpty = true,
                    displayedHeight = 400.dp,
                    refresh = {
                        homeAdapter.refresh()
                    },
                    requestButtonTextAndImg = false,
                )
            )
            doOnLayout {
                scrollToPosition(0)
            }
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = homeAdapter
        )
        binding.lsv.apply {
            makeScrollable()
            setRetryListener {
                if (mRealVM.bannerData.value.isNullOrEmpty()) {
                    mRealVM.getData()
                }
                homeAdapter.refresh()
            }
        }

        binding.btnCreation.setOnSingleClickListener {
            //if (AppNavigator.checkLoginIntercepted()) return@setOnSingleClickListener
            CreationRootActivity.start(activity)
        }
    }

    fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.feedFlow.collectLatest { pagingData ->
                homeAdapter.submitData(pagingData)
            }
        }

        mRealVM.bannerData.observe(this) {
            binding.banner.refreshData(it)
        }

        parentVm?.activityConfigCache?.observe(this) { config ->
            // 悬浮窗：始终显示
            handleFloatWindow(config.getFloatWindowInfo(ActivityConfigData.TAB_HOME))
            // 弹窗逻辑
            val popup = config.getPopupInfo(ActivityConfigData.TAB_HOME)
            // 关键拦截：只有首充没弹，才处理活动弹窗
            if (parentVm?.isFirstRechargeLogicActive == true) {
                Logger.d("Dialog", "拦截活动弹窗，等待下次回到首页再弹")
            } else {
                handlePopup(popup)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 场景 1：从其他 Activity 返回
        if (!isHidden) {
            checkAndTriggerPopup()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            resumePopup()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            resumePopup()
            checkAndTriggerPopup()
        }
    }

    private var lastCountTime = 0L

    private fun checkAndTriggerPopup() {
        val currentTime = System.currentTimeMillis()
        // 物理防抖：如果距离上次计数小于 500 毫秒，直接判定为重复触发
        if (currentTime - lastCountTime < 500) return
        lastCountTime = currentTime

        // 1. 获取当前次数并加 1
        val currentCount = SPUtils.getInstance().getInt(KEY_VISIT_COUNT, 0)
        val newCount = currentCount + 1

        // 2. 立即持久化新次数
        SPUtils.getInstance().put(KEY_VISIT_COUNT, newCount)

        // 3. 严格逻辑判断：仅在第 2 次时执行弹窗
        if (newCount == 2) {
            showTargetDialog()
        } else if (newCount > 2) {//超过2次直接显示悬浮窗，直到跳转到客服弹窗
            showFloatWindow()
        }
    }

    private fun showTargetDialog() {
        GuideAddCustomerServiceDialog.checkAndShow(GuideAddCustomerServiceDialog.SCENE_HOME) { dialog ->
            dialog?.setOnDismissListener {
                showFloatWindow()
            }
        }
    }

    private fun showFloatWindow() {
        if (!GuideAddCustomerServiceDialog.isHasJumped()) {
            Global.getGuideLinkCustomerInfo { list ->
                val targetCode = if (User.get().hasPaid()) {
                    SimpleActivityInfo.FLOAT_PAID
                } else {
                    SimpleActivityInfo.FLOAT_UNPAID
                }
                list?.find { it.businessCode == targetCode && it.isEnable() }?.let { info ->
                    handleFloatWindow(info)
                }
            }
        }
    }


    @Deprecated("这个活动暂时停用了")
    fun resumePopup() {
        if (parentVm?.isFirstRechargeLogicActive == false && parentVm?.hasActivityConfig == true) {
            handlePopup(parentVm?.activityConfigCache?.value?.getPopupInfo(ActivityConfigData.TAB_HOME))
            parentVm?.hasActivityConfig = false
        }
    }

    @Deprecated("这个活动暂时停用了")
    private fun handlePopup(popup: PopupInfo?) {
        popup?.let {
            if (popup.isShow()) {
                if (popup.businessType == 1) {
                    EventManager.onAppTrack(AppEvent.SF_SHOW_COURSE_ENROLL_POPUP)
                }
                ActivityDialog.show(popup)
                Config.get().activityPopupShowMark(popup.id)
            }
        }
    }

    @Deprecated("这个活动暂时停用了")
    private fun handleFloatWindow(float: FloatWindowInfo?) {
        if (float != null && float.isShow()) {
            binding.floatingBall.isVisible = true
            loadGifUrl(binding.ivFloating, float.imageUrl, R.drawable.img_default_cover_1_1.getResDrawable())
            Config.get().activityFloatShowMark(float.id)
            binding.floatingBall.setOnMfClickListener {
                AppRouter.jump(float.jumpUrl)
            }
        } else {
            binding.floatingBall.visibility = View.GONE
        }
    }

    private fun handleFloatWindow(info: SimpleActivityInfo?) {
        if (info != null && info.isEnable()) {
            if (binding.floatingBall.isVisible) return
            binding.floatingBall.isVisible = true
            loadGifUrl(binding.ivFloating, info.imageUrl, R.drawable.img_default_cover_1_1.getResDrawable())
            binding.floatingBall.setOnMfClickListener {
                AppRouter.jump(info.jumpUrl)
                GuideAddCustomerServiceDialog.markHasJumped()
                binding.floatingBall.visibility = View.GONE
            }
        } else {
            binding.floatingBall.visibility = View.GONE
        }
    }
}