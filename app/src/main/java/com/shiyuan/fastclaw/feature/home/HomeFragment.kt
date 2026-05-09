package com.shiyuan.fastclaw.feature.home

import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.blankj.utilcode.util.SPUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.startRefreshWithAdapter
import com.shiyuan.fastclaw.core.databinding.loadUri
import com.shiyuan.fastclaw.core.network.apis.entity.BannerInfo
import com.shiyuan.fastclaw.databinding.FragmentHomeBinding
import com.shiyuan.fastclaw.feature.main.MainViewModel
import com.shiyuan.fastclaw.router.AppRouter
import com.shiyuan.fastclaw.widget.decoration.StaggeredGridSpaceItemDecoration
import com.shiyuan.fastclaw.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.crop.util.ViewUtils
import com.shiyuan.fastclaw.core.manager.FlashLoginManager
import com.shiyuan.fastclaw.feature.login.LoginActivity.Companion.checkedPolicy
import com.shiyuan.fastclaw.feature.login.PrivacyPolicyDialog
import com.shiyuan.fastclaw.feature.login.flash.FlashLoginDialogFragment
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginDialog
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginViewModel
import com.shiyuan.fastclaw.feature.main.MainActivity
import com.shiyuan.fastclaw.feature.mine.MyShrimpChipsActivity
import com.shiyuan.fastclaw.feature.report.MyHistoryReportActivity
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.feature.settings.SettingsActivity
import com.umeng.socialize.bean.SHARE_MEDIA
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

    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnUploadResume -> {
                /*val mainActivity = activity as? MainActivity
                mainActivity?.switchToTab(1)*/
               // SettingsActivity.start(activity)
                UploadResumeDialog.show(
                    manager = this.parentFragmentManager,
                )
            }
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
        }

        mRealVM.bannerData.observe(this) {
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