package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants.SHRIMPSTICKS_URL
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.databinding.setLoadState
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.ActivityMyResumeBinding
import com.shiyuan.fastclaw.databinding.ActivityMyShrimpChipsBinding
import com.shiyuan.fastclaw.feature.report.ResumeListDialog
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.PASS
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.PENDING
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.RUNNING
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @Introduce: 我的虾条
 */
class MyResumeActivity : BaseViewModelActivity<ActivityMyResumeBinding, HomeReportViewModel>() {

    private val handler = Handler(Looper.getMainLooper())
    private val POLLING_INTERVAL = 2000L // 2秒
    private var resumePollingRunnable: Runnable? = null
    private var isResumePolling = false


    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, MyResumeActivity::class.java)
            activity.startActivity(intent)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
        mRealVM.queryLatestResume()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.tvUpload.setOnSingleClickListener {
            UploadCancelDialog.show(this.supportFragmentManager,{
                uploadResume()
            })
        }
    }

    fun  uploadResume(){
        //上传简历
        UploadResumeDialog.show(manager = this.supportFragmentManager) { fileInfo ->
            if (fileInfo != null) {
                mRealVM.submitResume(fileInfo.uri) {
                    mRealVM.setLoading(true)
                    mRealVM.resumeMaterialQuality(it ?: "", fileInfo.fileName ?: "")
                }
            }
        }
    }

    private fun initObservers() {
        mRealVM.resumeData.observe(this) {
            it?.let {
                binding.ll.visibility = if(it.hasLatestResume) View.VISIBLE else View.GONE
                if(it.hasLatestResume){
                    binding.tvTime.text = "上传于 ${it.submitTime}"
                    binding.tvName.text = it.resumeFileName ?: ""
                }
            }
        }

        mRealVM.resumeDetectionData.observe(this) { data ->
            Log.d("checkStatus", data.safeCheckStatus)
            when (data.safeCheckStatus) {
                PASS -> {
                    mRealVM.queryLatestResume()
                    "简历上传成功".showToast()
                    stopResumePolling()  // ✅ 停止轮询
                }

                RUNNING, PENDING -> {
                    if (!isResumePolling) {
                        startResumePolling()
                    }
                }
                else -> {
                    mRealVM.setLoading(false)
                    data.blockingIssuesMessage?.takeIf { it.isNotEmpty() }?.showToast() ?: "简历解析失败".showToast()
                    stopResumePolling()  // ✅ 停止轮询
                }
            }
        }
    }

    private fun startResumePolling() {
        if (isResumePolling) return  // ✅ 防止重复启动

        isResumePolling = true
        resumePollingRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && !isDestroyed && isResumePolling) {
                    Log.d("Polling", "轮询检查状态...")
                    mRealVM.resumeMaterialQualityr()
                    handler.postDelayed(this, POLLING_INTERVAL)
                }
            }
        }
        handler.postDelayed(resumePollingRunnable!!, POLLING_INTERVAL)
        Log.d("Polling", "开始轮询")
    }

    private fun stopResumePolling() {
        if (!isResumePolling) return  // ✅ 停止前检查

        isResumePolling = false
        resumePollingRunnable?.let {
            handler.removeCallbacks(it)
            resumePollingRunnable = null
        }
        Log.d("Polling", "停止轮询")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopResumePolling()
    }
}
