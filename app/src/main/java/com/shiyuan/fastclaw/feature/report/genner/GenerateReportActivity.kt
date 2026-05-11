package com.shiyuan.fastclaw.feature.report.genner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.Observable
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.LoadState
import com.shiyuan.fastclaw.databinding.ActivityMyHistoryReportBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.databinding.ActivityGennerReportBinding

/**
 * @Introduce: 历史报告列表页面
 */
class GenerateReportActivity : BaseViewModelActivity<ActivityGennerReportBinding, GenerateReportViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, GenerateReportActivity::class.java)
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

    }

    private fun initObservers() {

    }
}
