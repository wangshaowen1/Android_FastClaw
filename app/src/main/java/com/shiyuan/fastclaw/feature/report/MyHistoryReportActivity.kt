package com.shiyuan.fastclaw.feature.report

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.LoadState
import com.shiyuan.fastclaw.databinding.ActivityMyHistoryReportBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString

/**
 * @Introduce: 历史报告列表页面
 */
class MyHistoryReportActivity : BaseViewModelActivity<ActivityMyHistoryReportBinding, ReportViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, MyHistoryReportActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.srl.setOnRefreshListener {
            mRealVM.refresh()
        }

        binding.recyclerView.apply {
            layoutManager = ReportViewModel.buildLayoutManager(context)
            addItemDecoration(ReportViewModel.buildItemDecoration(context))
            adapter = mRealVM.listAdapter
        }

        binding.lsv.setRetryListener {
            mRealVM.refresh()
        }

        // 配置空视图
        binding.lsv.setDefaultEmptyImage(R.drawable.ic_not_song)
        binding.lsv.setNoDataPrompt(R.string.report_empty_tip.getResString())
    }

    private fun initObservers() {
        mRealVM.loadState.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                binding.srl.finishRefresh()
                when (val state = mRealVM.loadState.get()) {
                    is LoadState.Loading -> binding.lsv.showLoading()
                    is LoadState.NotLoading -> binding.lsv.showContent()
                    is LoadState.Empty -> binding.lsv.showEmpty()
                    is LoadState.Error -> binding.lsv.showRetry(state.error)
                    else -> { }
                }
            }
        })
    }
}
