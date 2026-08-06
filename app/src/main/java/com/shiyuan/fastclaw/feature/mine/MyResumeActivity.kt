package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.base.Joiner.on
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showAllowingStateLoss
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.viewmodel.UiState
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.databinding.setLoadState
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.FileUploadCredentialItemBody
import com.shiyuan.fastclaw.core.network.apis.request.FileUploadCredentialItemBody.Companion.BIZ_TYPE_CANDIDATE_ONLINE_RESUME_A
import com.shiyuan.fastclaw.core.network.apis.response.ResumeListData
import com.shiyuan.fastclaw.databinding.ActivityMyResumeBinding
import com.shiyuan.fastclaw.databinding.ActivityMyShrimpChipsBinding
import com.shiyuan.fastclaw.databinding.ItemMyResumeBinding
import com.shiyuan.fastclaw.feature.mine.dialog.AnalysisLoadingDialog
import com.shiyuan.fastclaw.feature.mine.dialog.UploadCancelDialog
import com.shiyuan.fastclaw.feature.report.ResumeListDialog
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.PASS
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.PENDING
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.RUNNING
import com.shiyuan.fastclaw.feature.report.report.HomeReportViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.widget.CommonLoadingDialog
import com.shiyuan.fastclaw.wxapi.WXEntryActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jsoup.internal.SoftPool
import kotlin.text.trim

/**
 * @Introduce: 我的虾条
 */
class MyResumeActivity : BaseViewModelActivity<ActivityMyResumeBinding, HomeReportViewModel>() {

    private val handler = Handler(Looper.getMainLooper())
    private val POLLING_INTERVAL = 2000L // 2秒
    private var resumePollingRunnable: Runnable? = null
    private var isResumePolling = false

    private val resumeAdapter by lazy {
        SingleBindingAdapter<ItemMyResumeBinding, ResumeListData>(
            R.layout.item_my_resume,
            emptyList()
        ) { itemBinding, _, item ->
            itemBinding.tvName.text = item.resumeName ?: item.fileName
            when (item.parseStatus) {
                2 -> {
                    itemBinding.tvName.setTextColor(resources.getColor(R.color.color_90_000000))
                    itemBinding.iv.setImageResource(R.drawable.ic_resume)
                    itemBinding.tvUpload.visibility = View.VISIBLE
                    itemBinding.tvUpload.setImageResource(R.drawable.ic_more)
                    itemBinding.tvUpload.setOnSingleClickListener {
                        UploadCancelDialog.show(supportFragmentManager) {
                            uploadResume(item.resumeTaskId)
                        }
                    }
                    itemBinding.tvTime.visibility = View.VISIBLE
                    itemBinding.tvTime.text =
                        getString(R.string.resume_upload_time, item.updateTime)
                    stopRotation(itemBinding.ivState)

                    itemBinding.llUploading.visibility = View.GONE
                }

                0, 1 -> {
                    itemBinding.tvName.setTextColor(resources.getColor(R.color.color_90_000000))
                    itemBinding.iv.setImageResource(R.drawable.ic_resume)
                    itemBinding.tvUpload.visibility = View.GONE

                    itemBinding.llUploading.visibility = View.VISIBLE
                    itemBinding.tvState.text = item.parseStatusText
                    startRotation(itemBinding.ivState)

                    itemBinding.tvTime.visibility = View.GONE
                }

                else -> {
                    itemBinding.tvName.setTextColor(resources.getColor(R.color.color_56_3c3c43))
                    itemBinding.iv.setImageResource(R.drawable.ic_resume2)
                    itemBinding.tvUpload.visibility = View.VISIBLE
                    itemBinding.tvUpload.setImageResource(R.drawable.ic_close4)
                    itemBinding.tvUpload.setOnSingleClickListener {
                        mRealVM.deleteResume(item.resumeTaskId)
                    }
                    itemBinding.tvTime.visibility = View.VISIBLE
                    itemBinding.tvTime.text = item.parseStatusText

                    itemBinding.llUploading.visibility = View.GONE
                    stopRotation(itemBinding.ivState)
                }
            }
        }
    }


    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, MyResumeActivity::class.java)
            activity.startActivity(intent)
            EventManager.onAppTrack(AppEvent.KLX_CAPP_MY_RESUME_PAGE)
        }
    }

    private var uploadResumeDialog: UploadResumeDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
        mRealVM.queryResumeList()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.rvResumeList.apply {
            layoutManager = LinearLayoutManager(this@MyResumeActivity)
            adapter = resumeAdapter
        }

        binding.btnUploadResume.setOnSingleClickListener {
            uploadResume()
        }
    }

    fun uploadResume(replaceResumeTaskId: String? = null) {
        //上传简历
        uploadResumeDialog =
            UploadResumeDialog.show(manager = this.supportFragmentManager, false, { fileInfo ->
                if (fileInfo != null) {
                    showLoading("上传中")
                    mRealVM.uploadFilesWithSecurityCheck(
                        listOf(fileInfo.uri), BIZ_TYPE_CANDIDATE_ONLINE_RESUME_A,
                        onSuccess = { successUrls ->
                            hideLoading()
                            mRealVM.submitResumeUpload(
                                successUrls.first(),
                                fileInfo.fileName,
                                replaceResumeTaskId,
                                false
                            ) {
                                hideLoading()
                            }
                        },
                        onFailure = {
                            hideLoading()
                            it.showToast()
                        })
                }
            }, {
                mRealVM.code.value = it
            })
    }

    override fun onResume() {
        super.onResume()
        if (!mRealVM.code.value.isNullOrEmpty()) {
            Log.d("uploadResumeDialog", mRealVM.code.value.toString())
            uploadResumeDialog?.dismissAllowingStateLoss()
            mRealVM.queryCodeResumeBinding(mRealVM.code.value ?: "") { resume ->
                mRealVM.submitResumeUpload(
                    resume?.resumeUrl,
                    resume?.resumeFileName,
                    resume?.resumeTaskBizId
                )
            }
        }
    }


    private var commonLoadingDialog: CommonLoadingDialog? = null

    fun showLoading(text: String? = null) {
        if (commonLoadingDialog?.isAdded == true) return
        commonLoadingDialog = CommonLoadingDialog.newInstance(text).apply {
            show(this@MyResumeActivity.supportFragmentManager)
        }
    }


    fun hideLoading() {
        commonLoadingDialog?.dismissAllowingStateLoss()
        commonLoadingDialog = null
    }


    private fun initObservers() {
        mRealVM.resumeListData.observe(this) { data ->
            val resumeList = data.orEmpty()
            val hasResume = resumeList.isNotEmpty()
            if (hasResume) {
                binding.tvTip.text = resources.getString(R.string.resume_tip)
                binding.rvResumeList.visibility = View.VISIBLE
                if (resumeList.none { it.parseStatus == 2 || it.parseStatus == 0 || it.parseStatus == 1 }) {
                    binding.btnUploadResume.visibility = View.VISIBLE
                } else {
                    binding.btnUploadResume.visibility = View.GONE
                }
            } else {
                binding.tvTip.text = resources.getString(R.string.resume_tip)
                binding.rvResumeList.visibility = View.GONE
                binding.btnUploadResume.visibility = View.VISIBLE
            }
            resumeAdapter.submitList(resumeList)
            val isParseStatus2Exists = resumeList.any { it.parseStatus == 0 || it.parseStatus == 1 }
            if (isParseStatus2Exists) {
                if (!isResumePolling) {
                    startResumePolling()
                }
            } else {
                stopResumePolling()  // ✅ 停止轮询
            }
        }

        mRealVM.resumeUploadResult.observe(this) { data -> //上传成功
            //解析简成功发送消息
            FlowEventBus.postSync(Events.ResumeEvent(true))
            startResumePolling()
        }
    }


    private var rotateAnim: RotateAnimation? = null
    private fun startRotation(view: View) {
        if (rotateAnim == null) {
            rotateAnim = RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 1000L
                repeatCount = Animation.INFINITE
                interpolator = LinearInterpolator()
            }
        }
        // 如果动画已经在运行，不要重复启动
        if (rotateAnim != null && view.animation != rotateAnim) {
            view.startAnimation(rotateAnim)
        }
    }

    private fun stopRotation(view: View) {
        view.clearAnimation()
    }


    private fun startResumePolling() {
        if (isResumePolling) return  // ✅ 防止重复启动

        isResumePolling = true
        resumePollingRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && !isDestroyed && isResumePolling) {
                    Log.d("Polling", "轮询检查状态...")
                    mRealVM.queryResumeList()
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
