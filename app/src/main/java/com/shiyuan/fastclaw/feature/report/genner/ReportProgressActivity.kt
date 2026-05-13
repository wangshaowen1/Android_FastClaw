package com.shiyuan.fastclaw.feature.report.genner

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.LinearInterpolator
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.nhsd.basic.BaseViewModelActivity
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportData
import com.shiyuan.fastclaw.databinding.ActivityReportProgressBinding
import com.shiyuan.fastclaw.feature.report.MyHistoryReportActivity

/**
 * @Introduce:
 */
class ReportProgressActivity : BaseViewModelActivity<ActivityReportProgressBinding, GenerateReportViewModel>() {

    companion object {
        fun start(activity: Activity?, resumeCheckId: String?,jdCheckId: String?,reportType: String?) {
            activity ?: return
            val intent = Intent(activity, ReportProgressActivity::class.java)
            intent.putExtra("resumeCheckId", resumeCheckId)
            intent.putExtra("jdCheckId", jdCheckId)
            intent.putExtra("reportType", reportType)
            activity.startActivity(intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val POLLING_INTERVAL = 2000L // 2秒


    private var isReportPolling = false
    private var reportPollingRunnable: Runnable? = null

    // 启动 JD 图片轮询
    private fun startJdPolling() {
        if (isReportPolling) return
        isReportPolling = true
        reportPollingRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && !isDestroyed && isReportPolling) {
                    mRealVM.interviewReportr()
                    handler.postDelayed(this, POLLING_INTERVAL)
                }
            }
        }
        handler.postDelayed(reportPollingRunnable!!, POLLING_INTERVAL)
    }

    private fun stopJdPolling() {
        if (!isReportPolling) return
        isReportPolling = false
        reportPollingRunnable?.let { handler.removeCallbacks(it) }
        reportPollingRunnable = null
    }

    private var rotationAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        mRealVM.interviewReport(InterviewReportData(
            resumeCheckId = intent.getStringExtra("resumeCheckId"),
            jdCheckId =   intent.getStringExtra("jdCheckId"),
            reportType = intent.getStringExtra("reportType")
        ))
        mRealVM.reportData.observe(this){
            when (it.reportStatus) {
                "SUCCESS " -> {
                    stopJdPolling()
                    MyHistoryReportActivity.start(this)
                }
                else -> {
                    startJdPolling()
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
    }


    override fun onDestroy() {
        rotationAnimator?.cancel()
        super.onDestroy()
        stopJdPolling()
    }
}
