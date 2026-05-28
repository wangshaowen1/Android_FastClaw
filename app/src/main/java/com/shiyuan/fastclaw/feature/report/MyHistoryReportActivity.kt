package com.shiyuan.fastclaw.feature.report

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ActivityMyHistoryReportBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.common.ext.startRefreshWithAdapter
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @Introduce: 历史报告列表页面
 */
class MyHistoryReportActivity :
    BaseViewModelActivity<ActivityMyHistoryReportBinding, ReportViewModel>() {

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
            ReportStatus.OTHER, ReportStatus.FAILED -> {
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
    }
}