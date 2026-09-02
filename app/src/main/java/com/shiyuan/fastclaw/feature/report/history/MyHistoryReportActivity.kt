package com.shiyuan.fastclaw.feature.report.history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.paging.LoadState
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ActivityMyHistoryReportBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.common.ext.startRefreshWithAdapter
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.shiyuan.fastclaw.feature.report.ReportViewModel

/**
 * @Introduce: 历史报告列表页面
 */
class MyHistoryReportActivity :
    BaseViewModelActivity<ActivityMyHistoryReportBinding, ReportViewModel>() {

    companion object {
        private const val REPORT_LIST_REFRESH_INTERVAL = 5000L

        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, MyHistoryReportActivity::class.java)
            activity.startActivity(intent)
        }
    }

    object ReportRefreshPolicy {

        fun shouldAutoRefresh(reports: List<ReportInfo>): Boolean {
            return reports.any { it.status == ReportStatus.IN_PROGRESS }
        }
    }


    private var reportListRefreshJob: Job? = null
    private var isReportListRefreshing = false

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

    private val mListAdapter by lazy {
        ReportAdapter() {
            onReportItemClick(it)
        }
    }

    private fun initView() {
        binding.recyclerView.apply {
            layoutManager = ReportViewModel.buildLayoutManager(context)
            addItemDecoration(ReportViewModel.buildItemDecoration(context))
            adapter = mListAdapter
        }
        binding.srl.startRefreshWithAdapter(
            mListAdapter,
            binding.lsv,
            onRefreshListener = {

            },
            addExtraLoadStateListener = {

            })
        // 配置空视图
        binding.lsv.setDefaultEmptyImage(R.drawable.ic_empty)
        binding.lsv.setNoDataPrompt(R.string.report_empty_tip.getResString())
    }

    /**
     * 报告 item 点击处理（原 ViewModel 中的逻辑移至 Activity）
     */
    private fun onReportItemClick(info: ReportInfo) {
        when (info.status) {
            ReportStatus.OTHER -> {
                EventManager.onAppTrack(AppEvent.KLX_CAPP_INTERVIEWREPORT_REPORT_PAGE)
                SimpleBrowserActivity.launch(
                    this,
                    info.detailUrl,
                )
            }
            else -> {

            }
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            mRealVM.reportListFlow.collectLatest { pagingData ->
                mListAdapter.submitData(pagingData)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    updateReportListAutoRefresh()
                    mListAdapter.loadStateFlow.collectLatest { loadStates ->
                        isReportListRefreshing = loadStates.refresh is LoadState.Loading
                        if (loadStates.refresh is LoadState.NotLoading) {
                            updateReportListAutoRefresh()
                        }
                    }
                } finally {
                    stopReportListAutoRefresh()
                }
            }
        }
    }

    private fun updateReportListAutoRefresh() {
        if (ReportRefreshPolicy.shouldAutoRefresh(mListAdapter.snapshot().items)) {
            startReportListAutoRefresh()
        } else {
            stopReportListAutoRefresh()
        }
    }

    private fun startReportListAutoRefresh() {
        if (reportListRefreshJob?.isActive == true) return
        reportListRefreshJob = lifecycleScope.launch {
            while (true) {
                delay(REPORT_LIST_REFRESH_INTERVAL)
                if (!isReportListRefreshing) {
                    mListAdapter.refresh()
                }
            }
        }
    }

    private fun stopReportListAutoRefresh() {
        reportListRefreshJob?.cancel()
        reportListRefreshJob = null
    }

    override fun onDestroy() {
        stopReportListAutoRefresh()
        super.onDestroy()
    }
}
