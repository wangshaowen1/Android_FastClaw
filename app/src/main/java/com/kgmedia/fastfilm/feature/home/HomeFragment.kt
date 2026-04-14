package com.kgmedia.fastfilm.feature.home

import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.blankj.utilcode.util.SPUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.databinding.loadUri
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.databinding.FragmentHomeBinding
import com.kgmedia.fastfilm.feature.main.MainViewModel
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.decoration.StaggeredGridSpaceItemDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.crop.util.ViewUtils
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhpan.indicator.enums.IndicatorStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
            itemAnimator = null
            adapter = homeAdapter
                .withLoadStateFooter(
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
        }
    }

    fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.feedFlow.collectLatest { pagingData ->
                homeAdapter.submitData(pagingData)
            }
        }

        // 首次数据加载完成后，确保从第一条开始显示
        // loadStateFlow.first() 只触发一次：refresh 状态变为 NotLoading 且列表有数据时
        lifecycleScope.launch {
            homeAdapter.loadStateFlow
                .filter { it.refresh is LoadState.NotLoading && homeAdapter.itemCount > 0 }
                .first()
            binding.recyclerView.scrollToPosition(0)
        }

        mRealVM.bannerData.observe(this) {
            binding.banner.refreshData(it)
        }
    }

    override fun onStart() {
        super.onStart()
        // 场景 1：从其他 Activity 返回
        binding.root.post {
            if (isActive && !isHidden) {
                checkAndTriggerPopup()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            checkAndTriggerPopup()
        }
    }


    private fun checkAndTriggerPopup() {
        // 1. 获取当前次数并加 1
        val currentCount = SPUtils.getInstance().getInt(KEY_VISIT_COUNT, 0)
        val newCount = currentCount + 1

        // 2. 立即持久化新次数
        if (newCount < 10) {//防止无限增长
            SPUtils.getInstance().put(KEY_VISIT_COUNT, newCount)
        }
    }
}