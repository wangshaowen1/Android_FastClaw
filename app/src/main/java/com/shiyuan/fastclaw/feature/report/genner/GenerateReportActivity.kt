package com.shiyuan.fastclaw.feature.report.genner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import com.blankj.utilcode.util.KeyboardUtils
import com.gyf.immersionbar.ImmersionBar
import com.kuaige.imageloader.GlideApp
import com.shiyuan.fastclaw.R
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showAllowingStateLoss
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.manager.FileManager
import com.nhsd.basic.viewmodel.UiState
import com.nhsd.mattise.Matisse
import com.nhsd.mattise.MimeType
import com.nhsd.mattise.engine.impl.GlideEngine
import com.nhsd.mattise.internal.entity.CaptureStrategy
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.Constants.Extra.IMAGE
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.common.permissions.requestImages2VideosPermissions
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.ActivityGennerReportBinding
import com.shiyuan.fastclaw.feature.mine.dialog.AnalysisLoadingDialog
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.feature.settings.feedback.FeedbackActivity.Companion.REQUEST_CODE_CHOOSE
import com.shiyuan.fastclaw.widget.PhotoGridView
import com.umeng.socialize.utils.UmengText

/**
 * @Introduce: 历史报告列表页面
 */
class GenerateReportActivity :
    BaseViewModelActivity<ActivityGennerReportBinding, GenerateReportViewModel>() {

    companion object {
        const val DATA = "data"
        const val PASS = "PASS"
        const val PENDING = "PENDING"
        const val RUNNING = "RUNNING"
        const val FAILED = "FAILED"
        const val JD_REPORT = "JD_REPORT"
        const val PERSONAL_REPORT = "PERSONAL_REPORT"
        const val TEXT = "TEXT"

        private val MAX_LENGTH = 2000

        fun start(activity: Activity?, data: ResumeData) {
            activity ?: return
            val intent = Intent(activity, GenerateReportActivity::class.java)
            intent.putExtra(DATA, data)
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
        data = intent.getParcelableExtra(DATA) as? ResumeData
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
            mRealVM.isJd.value  = it.isJd
            if (it.hasLatestResume) {
               // initStateView("简历上传成功", "PASS")
                mRealVM.resumeData.value = data?.apply {
                    blockingIssuesMessage = "简历上传成功"
                    checkStatus = PASS
                }
            } else {
                mRealVM.resumeData.value = data?.apply {
                    blockingIssuesMessage = "简历解析中"
                    checkStatus = PENDING
                }
                mRealVM.resumeMaterialQuality(it.resumeFileUrl ?: "", it.resumeFileName ?: "")
            }
        }
        binding.tvUpload.setOnSingleClickListener {
            //上传简历
            UploadResumeDialog.show(manager = supportFragmentManager,false, fileSelectedListener ={ fileInfo ->
                if (fileInfo != null) {
                    mRealVM.submitResume(fileInfo.uri) {
                        binding.tvResumeName.text = fileInfo.fileName
                        initStateView("简历解析中", PENDING)
                        mRealVM.resumeMaterialQuality(it, fileInfo.fileName ?: "")
                    }
                }
            })
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

        binding.tvSkip.setOnSingleClickListener {  //直接用简历
            if(mRealVM.resumeData.value?.checkStatus != PASS){
                mRealVM.resumeData.value?.blockingIssuesMessage?.showToast()
                return@setOnSingleClickListener
            }
            ReportProgressActivity.start(this, resumeCheckId =mRealVM.resumeData.value?.checkId,null,reportType =PERSONAL_REPORT)
        }
        binding.rlTrue.setOnSingleClickListener {//简历 +JD
            //校验简历要通过
            if(mRealVM.resumeData.value?.checkStatus != PASS){
                mRealVM.resumeData.value?.blockingIssuesMessage?.showToast()
                return@setOnSingleClickListener
            }

            //校验是否jd
            if(mRealVM.isJd.value == true){
                if(mRealVM.tableSelect.value == 0){ //选中截图
                    if(mRealVM.photoSize.value  == 0){
                        "请先添加目标岗位位信息".showToast()
                        return@setOnSingleClickListener
                    }
                      //校验截图
                    if(mRealVM.jdImageData.value?.checkStatus != PASS){
                        mRealVM.jdImageData.value?.blockingMessage?.showToast()
                        return@setOnSingleClickListener
                    }
                    ReportProgressActivity.start(this,resumeCheckId =mRealVM.resumeData.value?.checkId,jdCheckId = mRealVM.jdImageData.value?.checkId,reportType =JD_REPORT)
                }else{ //选中粘贴
                   if(binding.etTxt.text.isNullOrBlank()){
                       "粘贴公司名称与岗位职责信息".showToast()
                      return@setOnSingleClickListener
                   }
                  mRealVM.jdQualityQuality(null, binding.etTxt.text.toString(),TEXT)
                }
            }else{
                ReportProgressActivity.start(this, resumeCheckId =mRealVM.resumeData.value?.checkId,null,reportType =PERSONAL_REPORT)
            }
        }
        binding.rlSelect1.setOnSingleClickListener {mRealVM.tableSelect.postValue(0)  }
        binding.rlSelect2.setOnSingleClickListener{mRealVM.tableSelect.postValue(1)  }



        GlideApp.with(this)
            .asGif()
            .load(R.drawable.img_jc_gif)
            .into(binding.ivGif)

        binding.etTxt.textSelectHandle?.setTint(R.color.color_0099FF.getResColor())
        binding.etTxt.textSelectHandleLeft?.setTint(R.color.color_0099FF.getResColor())
        binding.etTxt.textSelectHandleRight?.setTint(R.color.color_0099FF.getResColor())
        binding.etTxt.doAfterTextChanged { editable ->
            val currentLength = editable?.length ?: 0
            binding.tvWordCount.text = "$currentLength/$MAX_LENGTH"
            // 达到2000字时提示
            if (currentLength == MAX_LENGTH) {
               "已达最大输入字数 $MAX_LENGTH 字".showToast()
            }
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
            PASS -> {
                // 处理通过状态
                binding.ivState.setImageResource(R.drawable.ic_up_success)
                binding.tvState.text = blockingIssuesMessage
                binding.tvState.setTextColor(resources.getColor(R.color.color_15D173))
                stopRotation()   // 停止旋转
                //解析简成功发送消息
                FlowEventBus.postSync(Events.ResumeEvent(true))
            }

            RUNNING, PENDING -> {
                // 处理运行中状态
                binding.ivState.setImageResource(R.drawable.ic_running)
                binding.tvState.text = "简历解析中"
                binding.tvState.setTextColor(resources.getColor(R.color.color_0099FF))
                startRotation()  // 开始旋转
            }

            else -> {
                // 处理失败
                binding.ivState.setImageResource(R.drawable.ic_up_failed)
                binding.tvState.text = blockingIssuesMessage
                binding.tvState.setTextColor(resources.getColor(R.color.color_F74C30))
                stopRotation()
            }
        }
    }

    private var rotateAnim: RotateAnimation? = null
    private fun startRotation() {
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
        if (rotateAnim != null && binding.ivState.animation != rotateAnim) {
            binding.ivState.startAnimation(rotateAnim)
        }
    }

    private fun stopRotation() {
        binding.ivState.clearAnimation()
    }

    private fun initObservers() {
        mRealVM.resumeData.observe(this) { data ->
            Log.d("checkStatus", data.safeCheckStatus)
            when (data.safeCheckStatus) {
                PASS -> {
                   // mRealVM.setLoading(false)
                    initStateView(data.blockingIssuesMessage?.takeIf { it.isNotEmpty() }
                        ?: "简历上传成功", data.safeCheckStatus)
                    stopResumePolling()  // ✅ 停止轮询
                }

                RUNNING, PENDING -> {
                    initStateView(data.blockingIssuesMessage?.takeIf { it.isNotEmpty() }
                        ?: "简历解析中", data.safeCheckStatus)
                    // ✅ 只有在未轮询且 checkId 存在时才启动
                    if (!isResumePolling && mRealVM.resumeData.value?.checkId != null) {
                        startResumePolling()
                    }
                }
                else -> {
                   // mRealVM.setLoading(false)
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
                PASS -> {
                    binding.llJdImageState.visibility = View.GONE
                    mRealVM.setLoading(false)
                    stopJdPolling()  // ✅ 停止轮询
                }

               RUNNING, PENDING -> {
                    binding.llJdImageState.visibility = View.GONE
                    // ✅ 只有在未轮询且 checkId 存在时才启动
                    if (!isJdPolling && mRealVM.jdImageData.value?.checkId != null) {
                        startJdPolling()
                    }
                }

                else -> {
                    mRealVM.setLoading(false)
                    // 处理失败
                    binding.llJdImageState.visibility = View.VISIBLE
                   // val message = data.blockingIssues?.joinToString("\n") ?: ""
                    binding.tvJdImageState.text = data.blockingMessage?.takeIf { it.isNotEmpty() } ?: "检测失败"
                    stopJdPolling()  // ✅ 停止轮询
                }
            }
        }

        mRealVM.jdTextData.observe(this) { data ->
            Log.d("checkStatus", data.safeCheckStatus)
            when (data.safeCheckStatus) {
                PASS -> {
                    mRealVM.setLoading(false)
                    binding.llJdTxtState.visibility = View.GONE
                    stopJdPolling()  // ✅ 停止轮询
                    ReportProgressActivity.start(this,resumeCheckId =mRealVM.resumeData.value?.checkId,jdCheckId = mRealVM.jdTextData.value?.checkId,reportType =JD_REPORT)
                }
                RUNNING, PENDING -> {
                    binding.llJdTxtState.visibility = View.GONE
                    // ✅ 只有在未轮询且 checkId 存在时才启动
                    if (!isJdPolling) {
                        startJdPolling()
                    }
                }

                else -> {
                    mRealVM.setLoading(false)
                    // 处理失败
                    binding.llJdTxtState.visibility = View.VISIBLE
                    // val message = data.blockingIssues?.joinToString("\n") ?: ""
                    binding.tvJdTxtState.text = data.blockingMessage?.takeIf { it.isNotEmpty() } ?: "检测失败"
                    stopJdPolling()  // ✅ 停止轮询
                }
            }
        }

        mRealVM.photoSize.observe(this) {
            Log.d("photoSize",it.toJsonString())
            if (it >= 1) {
                binding.llUp.visibility = View.GONE
                binding.photoGrid.visibility = View.VISIBLE
                // 强制 PhotoGridView 重新布局
                binding.photoGrid.post {
                    binding.photoGrid.requestLayout()
                    binding.photoGrid.invalidate()
                }
            } else {
                binding.llUp.visibility = View.VISIBLE
                binding.photoGrid.visibility = View.GONE

                binding.llJdImageState.visibility = View.GONE
            }
        }
        mRealVM.isJd.observe(this) {
            binding.llJd.visibility = if (it) View.VISIBLE else View.GONE
            binding.tvSkip.visibility = if (it) View.VISIBLE else View.GONE
        }
        mRealVM.tableSelect.observe(this){
            if(it == 0){
                binding.vBgSelect1.background = resources.getDrawable(R.drawable.bg_select_txt1)
                binding.tvSelect1.setTextColor(resources.getColor(R.color.color_1a1a1a))
                binding.rlImage.visibility = View.VISIBLE

                binding.vBgSelect2.background = resources.getDrawable(R.drawable.bg_select_txt2)
                binding.tvSelect2.setTextColor(resources.getColor(R.color.color_999999))
                binding.rlTxt.visibility = View.GONE


                binding.llJt.visibility = View.VISIBLE
                binding.llAll.background = resources.getDrawable(R.drawable.bg_genner_right_top_12)



            }else{
                binding.etTxt.post {
                    binding.etTxt.requestFocus()
                    // 把光标移动到末尾
                    binding.etTxt.text?.let { it1 -> binding.etTxt.setSelection(it1.length) }
                    // 阻止键盘弹出
                    val imm =getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.etTxt.windowToken, 0)
                }

                binding.vBgSelect1.background = resources.getDrawable(R.drawable.bg_select_txt3)
                binding.tvSelect1.setTextColor(resources.getColor(R.color.color_999999))
                binding.rlImage.visibility = View.GONE

                binding.vBgSelect2.background = resources.getDrawable(R.drawable.bg_select_txt4)
                binding.tvSelect2.setTextColor(resources.getColor(R.color.color_1a1a1a))
                binding.rlTxt.visibility = View.VISIBLE

                binding.llJt.visibility = View.GONE
                binding.llAll.background = resources.getDrawable(R.drawable.bg_genner_left_top_12)


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
                    mRealVM.jdMaterialQualityr(if (mRealVM.tableSelect.value == 0) IMAGE else TEXT)   // ★ 这里调用 JD 图片专用的检查方法
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
            Log.d("photoSize2",result.toJsonString())
            binding.photoGrid.setAll(result as ArrayList<Uri?>, false)
            //上传图片
            val nonNullUris = result.filterNotNull()
            mRealVM.submitMultipleFiles(nonNullUris) { successUrls, failedUris ->
                if (successUrls.isNotEmpty()) {
                    mRealVM.jdQualityQuality(successUrls, null, IMAGE)
                } else {
                    "图片上传".showToast()
                }
            }
        }
    }

    private var analysisLoadingDialog: AnalysisLoadingDialog? = null

    override fun onUiStateChanged(state: UiState) {
        if (state.isLoading) {
            if (analysisLoadingDialog?.isAdded != true) {
                analysisLoadingDialog = AnalysisLoadingDialog()
                analysisLoadingDialog?.showAllowingStateLoss(supportFragmentManager, "analysis_loading")
            }
        } else {
            analysisLoadingDialog?.dismissAllowingStateLoss()
            analysisLoadingDialog = null
        }
        state.pageError?.let { showPageError(it.message, it.retryAction) } ?: hidePageError()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopResumePolling()
        stopJdPolling()
    }
}
