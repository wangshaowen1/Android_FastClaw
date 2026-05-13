package com.shiyuan.fastclaw.feature.report.genner

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
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
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.manager.FileManager
import com.nhsd.mattise.Matisse
import com.nhsd.mattise.MimeType
import com.nhsd.mattise.engine.impl.GlideEngine
import com.nhsd.mattise.internal.entity.CaptureStrategy
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.common.permissions.requestImages2VideosPermissions
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.ActivityGennerReportBinding
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.feature.settings.feedback.FeedbackActivity.Companion.REQUEST_CODE_CHOOSE
import com.shiyuan.fastclaw.widget.PhotoGridView
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
    private val POLLING_INTERVAL = 2000L // 2秒

    private var resumePollingRunnable: Runnable? = null
    private var isResumePolling = false


    private var isJdPolling = false
    private var jdPollingRunnable: Runnable? = null

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
            if (it.hasLatestResume) {
                initStateView("简历上传成功", "PASS")
            } else {
                initStateView("简历解析中", "PENDING")
                mRealVM.resumeMaterialQuality(it.resumeFileUrl ?: "", it.resumeFileName ?: "")
            }
        }
        binding.tvUpload.setOnSingleClickListener {
            //上传简历
            UploadResumeDialog.show(manager = supportFragmentManager) { fileInfo ->
                if (fileInfo != null) {
                    mRealVM.submitResume(fileInfo.uri) {
                        initStateView("简历解析中", "PENDING")
                        mRealVM.resumeMaterialQuality(it, fileInfo.fileName ?: "")
                    }
                }
            }
        }
        binding.llUp.setOnSingleClickListener {
            requestAlbumsPermission()
        }

        binding.photoGrid.setOnPhotoChangedListener(object : PhotoGridView.OnPhotoChangedListener {
            override fun onPhotoAdded(photo: Uri?) {
                Log.d("onPhotoAdded", binding.photoGrid.originalPhotos.size.toString())
                mRealVM.photoSize.postValue(binding.photoGrid.originalPhotos.size)
            }

            override fun onPhotoRemoved(photo: Uri?) {
                Log.d("onPhotoRemoved", binding.photoGrid.originalPhotos.size.toString())
                mRealVM.photoSize.postValue(binding.photoGrid.originalPhotos.size)
            }

        })
        binding.photoGrid.setOnAddButtonClickedListener {
            requestAlbumsPermission()
        }
    }

    private fun requestAlbumsPermission() {
        requestImages2VideosPermissions {
            if (it) {
                //权限申请成功
                requestAlbums()
            } else {
                //权限申请失败
            }
        }
    }

    fun requestAlbums() {
        Matisse.from(this)
            .choose(MimeType.ofImage(), false)
            .countable(false)
            .capture(true)
            .captureStrategy(
                CaptureStrategy(true, FileManager.getInstance().fileProvider, "album")
            )
            .maxSelectable(3)
            .setSelectedUris(binding.photoGrid.copiedPhotos)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(GlideEngine())
            .showSingleMediaType(true)
            .originalEnable(Constants.ORIGINAL_ENABLE)
            .autoHideToolbarOnSingleTap(true)
            .forResult(REQUEST_CODE_CHOOSE)
    }


    fun initStateView(blockingIssuesMessage: String?, checkStatus: String) {
        when (checkStatus) {
            "PASS" -> {
                // 处理通过状态
                binding.ivState.setImageResource(R.drawable.ic_up_success)
                binding.tvState.text = blockingIssuesMessage
                binding.tvState.setTextColor(resources.getColor(R.color.color_15D173))
            }

            "RUNNING", "PENDING" -> {
                // 处理运行中状态
                binding.ivState.setImageResource(R.drawable.ic_running)
                binding.tvState.text = "简历解析中"
                binding.tvState.setTextColor(resources.getColor(R.color.color_0099FF))
            }

            else -> {
                // 处理失败
                binding.ivState.setImageResource(R.drawable.ic_up_failed)
                binding.tvState.text = blockingIssuesMessage
                binding.tvState.setTextColor(resources.getColor(R.color.color_F74C30))

            }
        }
    }

    private fun initObservers() {
        mRealVM.resumeData.observe(this) { data ->
            Log.d("checkStatus", data.safeCheckStatus)
            when (data.safeCheckStatus) {
                "PASS" -> {
                    initStateView(data.blockingIssuesMessage?.takeIf { it.isNotEmpty() }
                        ?: "简历上传成功", data.safeCheckStatus)
                    stopResumePolling()  // ✅ 停止轮询
                }

                "RUNNING", "PENDING" -> {
                    initStateView(data.blockingIssuesMessage?.takeIf { it.isNotEmpty() }
                        ?: "简历解析中", data.safeCheckStatus)
                    // ✅ 只有在未轮询且 checkId 存在时才启动
                    if (!isResumePolling && mRealVM.resumeData.value?.checkId != null) {
                        startResumePolling()
                    }
                }
                else -> {
                    // 处理失败
                    initStateView(data.blockingIssuesMessage?.takeIf { it.isNotEmpty() }
                        ?: "简历解析失败", data.safeCheckStatus)
                    stopResumePolling()  // ✅ 停止轮询
                }
            }
        }

        mRealVM.jdImageData.observe(this) { data ->
            Log.d("checkStatus", data.safeCheckStatus)
            when (data.safeCheckStatus) {
                "PASS" -> {
                    binding.llJdImageState.visibility = View.GONE
                    stopJdPolling()  // ✅ 停止轮询
                }

                "RUNNING", "PENDING" -> {
                    binding.llJdImageState.visibility = View.GONE
                    // ✅ 只有在未轮询且 checkId 存在时才启动
                    if (!isJdPolling && mRealVM.jdImageData.value?.checkId != null) {
                        startJdPolling()
                    }
                }

                else -> {
                    // 处理失败
                    binding.llJdImageState.visibility = View.VISIBLE
                    val message = data.blockingIssues?.joinToString("\n") ?: ""
                    binding.tvJdImageState.text = message
                    stopJdPolling()  // ✅ 停止轮询
                }
            }
        }
        mRealVM.photoSize.observe(this) {
            if (it > 0) {
                binding.llUp.visibility = View.GONE
                binding.llUp2.visibility = View.VISIBLE

            } else {
                binding.llUp.visibility = View.VISIBLE
                binding.llUp2.visibility = View.GONE

                binding.llJdImageState.visibility = View.GONE
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

    // 启动 JD 图片轮询
    private fun startJdPolling() {
        if (isJdPolling) return
        isJdPolling = true
        jdPollingRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && !isDestroyed && isJdPolling) {
                    Log.d("Polling", "JD图片轮询...")
                    mRealVM.jdMaterialQualityr()   // ★ 这里调用 JD 图片专用的检查方法
                    handler.postDelayed(this, POLLING_INTERVAL)
                }
            }
        }
        handler.postDelayed(jdPollingRunnable!!, POLLING_INTERVAL)
    }

    private fun stopJdPolling() {
        if (!isJdPolling) return
        isJdPolling = false
        jdPollingRunnable?.let { handler.removeCallbacks(it) }
        jdPollingRunnable = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            val result = Matisse.obtainResult(data)
            binding.photoGrid.setAll(result as ArrayList<Uri?>, false)
            //上传图片
            val nonNullUris = result.filterNotNull()
            mRealVM.submitMultipleFiles(nonNullUris) { successUrls, failedUris ->
                if (successUrls.isNotEmpty()) {
                    mRealVM.jdQualityQuality(successUrls, null,"IMAGE")
                } else {
                    "图片上传".showToast()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopResumePolling()
    }
}
