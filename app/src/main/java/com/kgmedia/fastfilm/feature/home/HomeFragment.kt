package com.kgmedia.fastfilm.feature.home

import android.view.View
import android.widget.ImageView
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.databinding.loadUri
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.databinding.FragmentHomeBinding
import com.kgmedia.fastfilm.feature.creator.CreationRootActivity
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.setOnSingleClickListener
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
        fun newInstance(): HomeFragment {
            val fragment = HomeFragment()
            return fragment
        }
    }

    private val homeAdapter by lazy {
        HomeAdapter()
    }
    private lateinit var bannerAdapter: BaseBannerAdapter<BannerInfo>

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
                    loadUri(image,data.imageUrl)
                    image.setOnSingleClickListener {
                        AppRouter.jump(data.jumpCode)
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
            addItemDecoration(GridSpacingItemDecoration(2, 10.dp, 0))
            adapter = homeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = homeAdapter,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
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
            if (AppNavigator.checkLoginIntercepted()) return@setOnSingleClickListener
            EventManager.onAppTrack(AppEvent.JG_CLICK_FADDISH_PAGE_CREATE_NOW_BUTTON)
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
    }
}