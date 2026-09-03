package com.shiyuan.fastclaw.feature.report.generation

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.lifecycle.lifecycleScope
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportData
import com.shiyuan.fastclaw.databinding.ActivityReportProgressBinding
import com.shiyuan.fastclaw.feature.main.MainActivity
import com.shiyuan.fastclaw.feature.shrimp.MyShrimpChipsActivity
import com.shiyuan.fastclaw.feature.report.history.MyHistoryReportActivity
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @Introduce:
 */
class ReportProgressActivity :
    BaseViewModelActivity<ActivityReportProgressBinding, GenerateReportViewModel>() {

    companion object {
        fun start(
            activity: Activity?,
            resumeCheckId: String?,
            jdCheckId: String?,
            reportType: String?
        ) {
            activity ?: return
            val intent = Intent(activity, ReportProgressActivity::class.java)
            intent.putExtra("resumeCheckId", resumeCheckId)
            intent.putExtra("jdCheckId", jdCheckId)
            intent.putExtra("reportType", reportType)
            activity.startActivity(intent)
            activity.finish()
            EventManager.onAppTrack(AppEvent.KLX_CAPP_INTERVIEWREPORT_LOADINGREPORT_PAGE)
        }
    }

    private var rotationAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        mRealVM.interviewReport(
            InterviewReportData(
                resumeCheckId = intent.getStringExtra("resumeCheckId"),
                jdCheckId = intent.getStringExtra("jdCheckId"),
                reportType = intent.getStringExtra("reportType")
            ), {
                lifecycleScope.launch {
                    delay(1000)
                    MyShrimpChipsActivity.start(this@ReportProgressActivity)
                    finish()
                }
            })
        mRealVM.reportData.observe(this) {
            when (it.reportStatus) {
                "SUCCESS","FAILED","CANCELLED" -> {
                    mRealVM.stopReportPolling()
                    MyHistoryReportActivity.start(this)
                    finish()
                }
                else -> {
                    binding.viewFlipper.startFlipping()
                    mRealVM.startReportPolling()
                }
            }
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        rotationAnimator = ObjectAnimator.ofFloat(binding.iv, "rotation", 0f, 360f).apply {
            duration = 1000
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        binding.tvTrue.setOnSingleClickListener {
            MyHistoryReportActivity.start(this)
            finish()
        }
    }


    override fun onDestroy() {
        rotationAnimator?.cancel()
        mRealVM.stopReportPolling()
        super.onDestroy()
    }
}
