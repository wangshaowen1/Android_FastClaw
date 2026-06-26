package com.shiyuan.fastclaw.feature.interview

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.manager.FileManager
import com.nhsd.mattise.Matisse
import com.nhsd.mattise.MimeType
import com.nhsd.mattise.engine.impl.GlideEngine
import com.nhsd.mattise.internal.entity.CaptureStrategy
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.common.permissions.PermissionsUtils
import com.shiyuan.fastclaw.common.permissions.requestImages2VideosPermissions
import com.shiyuan.fastclaw.common.permissions.requestRxPermissions
import com.shiyuan.fastclaw.common.speech.DashCosyVoiceStreamTtsPlayer
import com.shiyuan.fastclaw.common.speech.DashFunAsrSpeechTranscriber
import com.shiyuan.fastclaw.common.utils.JsonUtil
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.ext.getBundleValue
import com.shiyuan.fastclaw.databinding.ActivityInterviewBinding
import com.shiyuan.fastclaw.feature.information.JobSelectionActivity
import com.shiyuan.fastclaw.feature.interview.InterviewViewModel.Companion.ACTION_TARGET_JOB_SUBMIT
import com.shiyuan.fastclaw.feature.interview.InterviewViewModel.Companion.SESSION_STATUS_PREPARING
import com.shiyuan.fastclaw.feature.interview.InterviewViewModel.Companion.TARGET_JOB_INPUT_POSITION_SELECTED
import com.shiyuan.fastclaw.feature.interview.InterviewViewModel.Companion.TARGET_JOB_INPUT_UPLOAD_IMAGE
import com.shiyuan.fastclaw.feature.interview.bean.InterviewJobBean
import com.shiyuan.fastclaw.feature.interview.bean.InterviewRoomState
import com.shiyuan.fastclaw.feature.interview.dialog.InterviewFinishDialog
import com.shiyuan.fastclaw.feature.interview.dialog.InterviewJobDialog
import com.shiyuan.fastclaw.feature.settings.report.ReportActivity.Companion.REQUEST_CODE_CHOOSE
import com.shiyuan.fastclaw.router.AppNavigator
import java.util.UUID

/**
 * 模拟面试房间页面，
 * 负责展示面试消息流、目标岗位上传/选择、语音作答、TTS 播放以及结束面试确认。
 * @author：CaoWei
 * @date：2026/6/23 15:49
 */
class InterviewActivity : BaseViewModelActivity<ActivityInterviewBinding, InterviewViewModel>() {

    /** 模拟面试会话 ID，进入或恢复房间时使用。 */
    private val mInterviewId by lazy { intent.getBundleValue(INTERVIEW_ID, "") }

    /** 是否从可恢复面试入口进入。 */
    private val mIsRecover by lazy { intent.getBundleValue(IS_RECOVER, false) }

    /** 面试房间消息列表适配器。 */
    private val msgAdapter by lazy { InterviewRoomMessageAdapter() }

    /** 百炼语音识别器，按需初始化并在页面销毁时释放。 */
    private var speechTranscriber: DashFunAsrSpeechTranscriber? = null

    /** TTS 播放器，用于播放面试官问题。 */
    private var ttsPlayer: DashCosyVoiceStreamTtsPlayer? = null

    /** 结束录音后是否需要把最终识别文本提交为答案。 */
    private var shouldSendVoiceResult = false

    /** 当前语音识别得到的临时文本。 */
    private var pendingVoiceText: String? = null

    /** 当前等待提交的作答结束原因。 */
    private var pendingAnswerEndReason = InterviewViewModel.ANSWER_END_USER_SUBMIT

    /** 本地作答录音状态。 */
    private var answerRecording = false

    /** 是否等待结束语音播报完成后自动关闭页面。 */
    private var finishAfterTts = false

    /** 当前是否正在播放 TTS。 */
    private var ttsPlaying = false

    /** 等待下一题时按钮图片旋转动画。 */
    private var waitLoadingAnimator: ObjectAnimator? = null

    /** 用户主动发送后,强制把列表滚到最新一条(不受当前是否已在底部影响) */
    private var forceScrollToBottom = false

    companion object {
        private const val TAG = "InterviewActivity"
        private const val INTERVIEW_ID = "interviewId"
        private const val IS_RECOVER = "isRecover"

        /**
         * 打开模拟面试房间。
         */
        fun start(activity: Activity?, interviewId: String?, isRecover: Boolean? = false) {
            val intent = Intent(activity, InterviewActivity::class.java)
            intent.putExtra(INTERVIEW_ID, interviewId)
            intent.putExtra(IS_RECOVER, isRecover)
            activity?.startActivity(intent)
        }
    }

    /**
     * 初始化页面、数据观察和跨页面事件监听。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeData()
        observeEvents()
    }

    /**
     * 初始化消息列表、底部操作区点击事件，并进入或恢复面试房间。
     */
    private fun initView() {
        binding.rvMessage.layoutManager = LinearLayoutManager(this) //.apply { stackFromEnd = true }
        binding.rvMessage.adapter = msgAdapter
        binding.ivBack.setOnSingleClickListener {
            showFinishDialog()
        }
        binding.btnTargetJob.setOnSingleClickListener {
            requestAlbumPermission()
        }
        binding.ivVoiceIcon.setOnSingleClickListener {
            toggleAnswerRecording()
        }
        binding.tvVoicePress.setOnSingleClickListener {
            toggleAnswerRecording()
        }
        binding.btnFinishInterview.setOnSingleClickListener {
            showFinishDialog()
        }
        binding.tvJobHelp.setOnSingleClickListener {
            showTargetJobDialog()
        }
        binding.btnNoTargetJob.setOnSingleClickListener {
            JobSelectionActivity.start(this,
                true,
                title = getString(R.string.interview_select_mock_job_title),
                multipleNum = 1,
                type = "Interview"
            )
        }
        mRealVM.enterOrRecover(mInterviewId, mIsRecover)
    }

    /**
     * 观察面试房间 ViewModel 数据，刷新消息、状态、语音播放和上传结果。
     */
    private fun observeData() {
        mRealVM.roomState.observe(this) {
            updateRoomState(it)
        }
        mRealVM.sessionStatus.observe(this) {
            updateSessionStatus(it)
        }
        mRealVM.answerActionText.observe(this) {
            binding.tvVoicePress.text = it
        }
        mRealVM.answerDurationText.observe(this) {
            binding.tvVoiceDuration.text = it
            binding.tvVoiceDuration.visibility = if (it.isNotEmpty()) VISIBLE else INVISIBLE
        }
        mRealVM.answerTimeoutEvent.observe(this) {
            if (answerRecording) {
                answerRecording = false
                finishVoiceRecognizing(
                    canceled = false,
                    endReason = InterviewViewModel.ANSWER_END_TIMEOUT_AUTO_SUBMIT,
                )
            }
        }
        mRealVM.toastMessage.observe(this) {
            showMessage(it)
        }
        mRealVM.ttsSpeakEvent.observe(this) {
            ttsPlaying = ensureTtsPlayer().speak(it)
        }
        mRealVM.finishRoomEvent.observe(this) {
            //finish()
        }
        mRealVM.finishAfterTtsEvent.observe(this) {
            finishAfterTts = true
            if (!ttsPlaying) {
                finish()
            }
        }
        mRealVM.messageList.observe(this) { list ->
            // 是否滚到最新：用户刚发送(forceScrollToBottom)强制滚动；否则仅在原本已在底部时
            // 跟随新消息/流式增量，避免打断向上浏览历史。判定须在 submitList 之前取(反映旧位置)。
            val scrollToLatest = forceScrollToBottom || !binding.rvMessage.canScrollVertically(1)
            forceScrollToBottom = false
            msgAdapter.submitList(list)
            if (list.isNotEmpty()) {
                // 跳转 + 补偿滚动距离，确保刚插入(尚未测量高度)的最后一条完整贴底
                RecyclerViewScrollHelper.scrollToBottom(binding.rvMessage, 100)
            }
        }
        mRealVM.uploadList.observe(this) {
            //目标岗位上传成功
            mRealVM.sendAction(
                actionType = ACTION_TARGET_JOB_SUBMIT,
                payload = mapOf(
                    "targetJobInputType" to TARGET_JOB_INPUT_UPLOAD_IMAGE,
                    "uploadBatchId" to UUID.randomUUID().toString(),
                    "imageOssKeys" to it,
                )
            )
        }
    }

    /**
     * 监听岗位选择页回传事件，并把选中的目标岗位提交给面试房间。
     */
    private fun observeEvents() {
        observeEvents {
            on<Events.InterviewEvent> { event ->
                val dataList = JsonUtil.parseList(event.msgGson, InterviewJobBean::class.java)
                val dataItem = dataList?.firstOrNull() ?: return@on
                mRealVM.sendAction(
                    actionType = ACTION_TARGET_JOB_SUBMIT,
                    payload = mapOf(
                        "targetJobInputType" to TARGET_JOB_INPUT_POSITION_SELECTED,
                        "jobPositionCode" to dataItem.code,
                        "jobPositionName" to dataItem.name,
                    )
                )
            }
        }
    }

    /**
     * 根据面试房间状态切换底部上传、等待、作答和结束区域。
     */
    private fun updateRoomState(state: InterviewRoomState) {
        when (state) {
            InterviewRoomState.STARTING,
            InterviewRoomState.WAITING_TARGET_JOB -> {
                binding.layoutUpload.visible()
                binding.layoutWait.gone()
                binding.layoutAnswer.gone()
                binding.layoutFinish.gone()
                stopWaitAnimation()
            }
            InterviewRoomState.TARGET_JOB_PARSING,
            InterviewRoomState.PLAN_GENERATING,
            InterviewRoomState.ANSWER_SUBMITTED -> {
                binding.layoutWait.visible()
                binding.layoutUpload.gone()
                binding.layoutAnswer.gone()
                binding.layoutFinish.gone()
                startWaitAnimation()
            }
            InterviewRoomState.TARGET_JOB_PARSE_SUCCESS,
            InterviewRoomState.PLAN_READY,
            InterviewRoomState.WAITING_USER_ANSWER,
            InterviewRoomState.WAITING_FOLLOW_UP_DECISION,
            InterviewRoomState.ANSWERING -> {
                binding.layoutAnswer.visible()
                binding.layoutWait.gone()
                binding.layoutUpload.gone()
                binding.layoutFinish.gone()
                stopWaitAnimation()
            }
            InterviewRoomState.TARGET_JOB_PARSE_FAILED -> {
                binding.layoutUpload.visible()
                binding.layoutWait.gone()
                binding.layoutAnswer.gone()
                binding.layoutFinish.gone()
                stopWaitAnimation()
                binding.tvTargetJobTitle.text = getString(R.string.reupload)
            }
            InterviewRoomState.REPORT_GENERATING,
            InterviewRoomState.FINISHED,
            InterviewRoomState.CANCELED -> {
                binding.layoutFinish.visible()
                binding.layoutWait.gone()
                binding.layoutUpload.gone()
                binding.layoutAnswer.gone()
                stopWaitAnimation()
            }
            InterviewRoomState.ERROR -> stopWaitAnimation()
        }
    }

    /**
     * 根据会话状态刷新标题和状态标签颜色。
     */
    private fun updateSessionStatus(sessionStatus: String?) {
        if (sessionStatus == SESSION_STATUS_PREPARING) {
            binding.tvTitle.text = getString(R.string.interview_room_title_prepare)
            binding.tvStatus.setShapeSolidColor(ContextCompat.getColor(this, R.color.color_f5aa5e))
            binding.tvStatus.setUseShape()
        } else {
            binding.tvTitle.text = getString(R.string.interview_room_title_interviewing)
            binding.tvStatus.setShapeSolidColor(ContextCompat.getColor(this, R.color.color_57c57e))
            binding.tvStatus.setUseShape()
        }
    }

    private fun startWaitAnimation() {
        if (waitLoadingAnimator?.isStarted == true) return
        waitLoadingAnimator = ObjectAnimator.ofFloat(binding.ivWaitLoading, "rotation", 0f, 360f).apply {
            duration = 1000L
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    private fun stopWaitAnimation() {
        waitLoadingAnimator?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        waitLoadingAnimator = null
        binding.ivWaitLoading.rotation = 0f
    }

    /**
     * 切换作答录音状态：未录音时开始识别，录音中时结束并提交识别文本。
     */
    private fun toggleAnswerRecording() {
        if (mRealVM.roomState.value == InterviewRoomState.PLAN_GENERATING ||
            mRealVM.roomState.value == InterviewRoomState.ANSWER_SUBMITTED) return
        if (answerRecording) {
            answerRecording = false
            // 点击结束回答后等待 ASR 返回最终文本，计时先停，文本由回调发送。
            mRealVM.stopTiming()
            finishVoiceRecognizing(canceled = false)
            return
        }
        if (startVoiceRecognizing()) {
            if (!mRealVM.startAnswer()) {
                speechTranscriber?.cancel()
                return
            }
            answerRecording = true
            // 面试作答是点击开始，不走聊天页的按住录音浮层。
            updateVoiceActionRecording(true)
            mRealVM.startTiming()
        }
    }

    /**
     * 检查录音权限并启动语音识别。
     */
    private fun startVoiceRecognizing(): Boolean {
        if (AppNavigator.checkLoginIntercepted(false)) return false
        if (!PermissionsUtils.checkPermission(this, Manifest.permission.RECORD_AUDIO)) {
            requestRxPermissions(getString(R.string.toast_open_mike_permission), Manifest.permission.RECORD_AUDIO) { granted ->
                if (!granted) showMessage(getString(R.string.toast_open_mike_permission))
            }
            return false
        }
        ttsPlayer?.stop()
        pendingVoiceText = null
        shouldSendVoiceResult = false
        pendingAnswerEndReason = InterviewViewModel.ANSWER_END_USER_SUBMIT
        return runCatching {
            ensureSpeechTranscriber().start()
        }.getOrElse { throwable ->
            Log.d(TAG, "start voice recognizing failed", throwable)
            showMessage(getString(R.string.interview_voice_start_failed))
            false
        }
    }

    /**
     * 结束或取消语音识别；正常结束时等待最终识别结果并提交。
     */
    private fun finishVoiceRecognizing(
        canceled: Boolean,
        endReason: String = InterviewViewModel.ANSWER_END_USER_SUBMIT,
    ) {
        if (canceled) {
            shouldSendVoiceResult = false
            pendingVoiceText = null
            pendingAnswerEndReason = InterviewViewModel.ANSWER_END_USER_SUBMIT
            speechTranscriber?.cancel()
            resetVoiceRecognizingState()
            return
        }
        shouldSendVoiceResult = true
        pendingAnswerEndReason = endReason
        if (sendVoiceMessage(endReason)) {
            speechTranscriber?.cancel()
        } else {
            speechTranscriber?.stop()
        }
        resetVoiceRecognizingState()
    }

    /**
     * 懒加载面试语音识别器，并处理识别回调。
     */
    private fun ensureSpeechTranscriber(): DashFunAsrSpeechTranscriber {
        return speechTranscriber ?: DashFunAsrSpeechTranscriber(
            context = applicationContext,
            config = DashFunAsrSpeechTranscriber.Config(
                apiKey = BuildConfig.DASHSCOPE_API_KEY,
                url = BuildConfig.DASHSCOPE_SPEECH_URL,
            ),
            callback = object : DashFunAsrSpeechTranscriber.Callback {
                override fun onPartialResult(text: String) {
                    pendingVoiceText = text
                }

                override fun onFinalResult(text: String) {
                    if (isDestroyed) return
                    pendingVoiceText = text
                    if (shouldSendVoiceResult) {
                        sendVoiceMessage(pendingAnswerEndReason)
                    }
                }

                override fun onError(message: String) {
                    if (isDestroyed) return
                    if (shouldSendVoiceResult && !sendVoiceMessage(pendingAnswerEndReason)) {
                        showMessage(message)
                    } else if (!shouldSendVoiceResult) {
                        showMessage(message)
                    }
                    shouldSendVoiceResult = false
                    pendingAnswerEndReason = InterviewViewModel.ANSWER_END_USER_SUBMIT
                    resetVoiceRecognizingState()
                }

                override fun onStopped() {
                    if (isDestroyed) return
                    if (shouldSendVoiceResult && !sendVoiceMessage(pendingAnswerEndReason)) {
                        showMessage(getString(R.string.interview_voice_no_content))
                    }
                    shouldSendVoiceResult = false
                    pendingAnswerEndReason = InterviewViewModel.ANSWER_END_USER_SUBMIT
                    resetVoiceRecognizingState()
                }
            },
        ).also { speechTranscriber = it }
    }

    /**
     * 懒加载 TTS 播放器，用于播放面试官问题。
     */
    private fun ensureTtsPlayer(): DashCosyVoiceStreamTtsPlayer {
        return ttsPlayer ?: DashCosyVoiceStreamTtsPlayer(
            context = applicationContext,
            configProvider = {
                DashCosyVoiceStreamTtsPlayer.Config(
                    apiKey = mRealVM.ttsToken.value?.takeIf { it.isNotBlank() } ?: BuildConfig.DASHSCOPE_API_KEY,
                    url = BuildConfig.DASHSCOPE_SPEECH_URL,
                )
            },
            callback = object : DashCosyVoiceStreamTtsPlayer.Callback {
                override fun onCompleted() {
                    ttsPlaying = false
                    if (finishAfterTts) {
                        finish()
                    }
                }

                override fun onError(message: String) {
                    ttsPlaying = false
                    Log.d(TAG, "tts play failed: $message")
                    if (finishAfterTts) {
                        finish()
                    } else {
                        showMessage(message)
                    }
                }
            },
        ).also { ttsPlayer = it }
    }

    /**
     * 提交当前已识别到的语音文本。
     */
    private fun sendVoiceMessage(endReason: String = pendingAnswerEndReason): Boolean {
        val text = pendingVoiceText?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        pendingVoiceText = null
        shouldSendVoiceResult = false
        pendingAnswerEndReason = InterviewViewModel.ANSWER_END_USER_SUBMIT
        mRealVM.submitAnswer(text, endReason)
        return true
    }

    /**
     * 重置本地语音识别和作答计时状态。
     */
    private fun resetVoiceRecognizingState() {
        answerRecording = false
        updateVoiceActionRecording(false)
        mRealVM.stopTiming()
    }

    /**
     * 刷新底部作答按钮图标和声波动画。
     */
    private fun updateVoiceActionRecording(recording: Boolean) {
        binding.ivVoiceIcon.setImageResource(if (recording) R.drawable.img_interview_voice_icon_recording
            else R.drawable.img_interview_voice_icon_start
        )
        if (recording) {
            binding.ivVoiceBadge.startWaveAnimation()
        } else {
            binding.ivVoiceBadge.stopWaveAnimation()
        }
    }

    /**
     * 展示结束面试确认弹窗，确认后强制结束面试。
     */
    fun showFinishDialog() {
        val dialog = InterviewFinishDialog(this).showDialog()
        dialog.listener = {
            finishVoiceRecognizing(canceled = true)
            mRealVM.forceFinish {
                it?.message?.showToast()
                finish()
            }
        }
    }

    /**
     * 展示目标岗位截图帮助弹窗。
     */
    fun showTargetJobDialog() {
        InterviewJobDialog(this).showDialog()
    }

    /**
     * 请求相册/图片权限，授权后打开图片选择器。
     */
    private fun requestAlbumPermission() {
        requestImages2VideosPermissions {
            if (it) openAlbum()
        }
    }

    /**
     * 打开图片选择器，选择目标岗位截图。
     */
    private fun openAlbum() {
        Matisse.from(this)
            .choose(MimeType.ofImage(), false)
            .countable(false)
            .capture(true)
            .captureStrategy(CaptureStrategy(true, FileManager.getInstance().fileProvider, "album"))
            .maxSelectable(3)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(GlideEngine())
            .showSingleMediaType(true)
            .originalEnable(Constants.ORIGINAL_ENABLE)
            .autoHideToolbarOnSingleTap(true)
            .forResult(REQUEST_CODE_CHOOSE)
    }

    /**
     * 接收图片选择结果，并上传目标岗位截图。
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            mRealVM.uploadTarget(Matisse.obtainResult(data).orEmpty())
        }
    }

    /**
     * 系统返回键统一走结束面试确认弹窗。
     */
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        showFinishDialog()
    }

    /**
     * 释放语音识别、TTS 和声波动画资源。
     */
    override fun onDestroy() {
        stopWaitAnimation()
        binding.ivVoiceBadge.stopWaveAnimation()
        speechTranscriber?.release()
        speechTranscriber = null
        ttsPlayer?.release()
        ttsPlayer = null
        super.onDestroy()
    }
}
