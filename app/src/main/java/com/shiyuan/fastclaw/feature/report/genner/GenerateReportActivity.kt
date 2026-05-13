package com.shiyuan.fastclaw.feature.report.genner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.databinding.Observable
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.LoadState
import com.shiyuan.fastclaw.databinding.ActivityMyHistoryReportBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.ActivityGennerReportBinding
import com.umeng.analytics.pro.da
import kotlinx.serialization.json.Json

/**
 * @Introduce: 历史报告列表页面
 */
class GenerateReportActivity :
    BaseViewModelActivity<ActivityGennerReportBinding, GenerateReportViewModel>() {

    companion object {
        fun start(activity: Activity?, data: ResumeData) {
            activity ?: return
            val intent = Intent(activity, GenerateReportActivity::class.java)
            intent.putExtra("data", data)
            activity.startActivity(intent)
        }
    }

    var data: ResumeData? = null

    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private val POLLING_INTERVAL = 2000L // 2秒

    private var isPolling = false  // ✅ 添加轮询标志

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = intent.getParcelableExtra("data") as? ResumeData
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
        data?.let {
            Log.d("ResumeData", it.toJsonString())
            binding.tvResumeName.text = it.resumeFileName
            binding.llJd.visibility = if (it.isJd) View.VISIBLE else View.GONE
            if (it.checkStatus == null) {  //是上传 则是上传中开始的时候
                it.checkStatus = "RUNNING"
                it.blockingIssuesMessage = "简历解析中"
            }
            if (it.checkId == null) {  //检测
                mRealVM.resumeMaterialQuality(it.resumeFileUrl ?: "")
            }
            mRealVM.resumeData.postValue(it)
        }
    }


    private fun initObservers() {
        mRealVM.resumeData.observe(this) { data ->
            Log.d("checkStatus", data.safeCheckStatus)
            when (data.safeCheckStatus) {
                "PASS" -> {
                    // 处理通过状态
                    binding.ivState.setImageResource(R.drawable.ic_up_success)
                    binding.tvState.text = data.safeBlockingIssuesMessage
                    binding.tvState.setTextColor(R.color.color_15D173)
                    stopPolling()  // ✅ 停止轮询
                }

                "RUNNING" -> {
                    // 处理运行中状态
                    binding.ivState.setImageResource(R.drawable.ic_running)
                    binding.tvState.text = data.safeBlockingIssuesMessage
                    binding.tvState.setTextColor(R.color.color_0099FF)
                    // ✅ 只有在未轮询且 checkId 存在时才启动
                    if (!isPolling && mRealVM.resumeData.value?.checkId != null) {
                      //  startPolling()
                    }
                }

                else -> {
                    // 处理失败
                    binding.ivState.setImageResource(R.drawable.ic_up_failed)
                    binding.tvState.text = data.safeBlockingIssuesMessage
                    binding.tvState.setTextColor(R.color.color_F74C30)
                    stopPolling()  // ✅ 停止轮询
                }
            }
        }
    }


    private fun startPolling() {
        if (isPolling) return  // ✅ 防止重复启动

        isPolling = true
        pollingRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && !isDestroyed && isPolling) {
                    Log.d("Polling", "轮询检查状态...")
                    mRealVM.resumeMaterialQualityr()
                    handler.postDelayed(this, POLLING_INTERVAL)
                }
            }
        }
        handler.postDelayed(pollingRunnable!!, POLLING_INTERVAL)
        Log.d("Polling", "开始轮询")
    }

    private fun stopPolling() {
        if (!isPolling) return  // ✅ 停止前检查

        isPolling = false
        pollingRunnable?.let {
            handler.removeCallbacks(it)
            pollingRunnable = null
        }
        Log.d("Polling", "停止轮询")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }
}
