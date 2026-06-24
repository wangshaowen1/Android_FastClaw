package com.shiyuan.fastclaw.feature.interview

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
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
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.core.ext.getBundleValue
import com.shiyuan.fastclaw.databinding.ActivityInterviewBinding
import com.shiyuan.fastclaw.feature.interview.InterviewViewModel.Companion.ACTION_TARGET_JOB_SUBMIT
import com.shiyuan.fastclaw.feature.interview.InterviewViewModel.Companion.SESSION_STATUS_PREPARING
import com.shiyuan.fastclaw.feature.interview.InterviewViewModel.Companion.TARGET_JOB_INPUT_UPLOAD_IMAGE
import com.shiyuan.fastclaw.feature.interview.bean.InterviewRoomState
import com.shiyuan.fastclaw.feature.interview.dialog.InterviewFinishDialog
import com.shiyuan.fastclaw.feature.interview.dialog.InterviewJobDialog
import com.shiyuan.fastclaw.feature.settings.report.ReportActivity.Companion.REQUEST_CODE_CHOOSE
import com.shiyuan.fastclaw.router.AppNavigator
import java.util.UUID

class InterviewActivity : BaseViewModelActivity<ActivityInterviewBinding, InterviewViewModel>() {

    private val mInterviewId by lazy { intent.getBundleValue(INTERVIEW_ID, "") }
    private val mIsRecover by lazy { intent.getBundleValue(IS_RECOVER, false) }
    private val msgAdapter by lazy { InterviewRoomMessageAdapter() }
    private var speechTranscriber: DashFunAsrSpeechTranscriber? = null
    private var ttsPlayer: DashCosyVoiceStreamTtsPlayer? = null
    private var shouldSendVoiceResult = false
    private var pendingVoiceText: String? = null
    private var answerRecording = false
    /** 用户主动发送后,强制把列表滚到最新一条(不受当前是否已在底部影响) */
    private var forceScrollToBottom = false

    companion object {
        private const val TAG = "InterviewActivity"
        private const val INTERVIEW_ID = "interviewId"
        private const val IS_RECOVER = "isRecover"

        fun start(activity: Activity?, interviewId: String?, isRecover: Boolean? = false) {
            val intent = Intent(activity, InterviewActivity::class.java)
            intent.putExtra(INTERVIEW_ID, interviewId)
            intent.putExtra(IS_RECOVER, isRecover)
            activity?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeData()
    }

    private fun initView() {
        binding.rvMessage.layoutManager = LinearLayoutManager(this) //.apply { stackFromEnd = true }
        binding.rvMessage.adapter = msgAdapter
        binding.ivBack.setOnSingleClickListener {
            showFinishDialog()
        }
        binding.btnTargetJob.setOnSingleClickListener {
            requestAlbumPermission()
        }
        binding.layoutAnswer.setOnSingleClickListener {
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
        mRealVM.enterOrRecover(mInterviewId, mIsRecover)
    }

    private fun observeData() {
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
        mRealVM.toastMessage.observe(this) {
            showMessage(it)
        }
        mRealVM.ttsSpeakEvent.observe(this) {
            //ensureTtsPlayer().speak(it)
        }
        mRealVM.finishRoomEvent.observe(this) {
            //finish()
        }
    }

    private fun updateRoomState(state: InterviewRoomState) {
        binding.layoutPrepare.gone()
        binding.layoutAnswer.gone()
        binding.layoutFinish.gone()
        when (state) {
            InterviewRoomState.STARTING,
            InterviewRoomState.WAITING_TARGET_JOB,
            InterviewRoomState.TARGET_JOB_PARSE_SUCCESS -> {
                binding.layoutPrepare.visible()
            }
            InterviewRoomState.TARGET_JOB_PARSING,
            InterviewRoomState.PLAN_GENERATING,
            InterviewRoomState.PLAN_READY,
            InterviewRoomState.WAITING_USER_ANSWER,
            InterviewRoomState.ANSWER_SUBMITTED,
            InterviewRoomState.WAITING_FOLLOW_UP_DECISION,
            InterviewRoomState.ANSWERING -> {
                binding.layoutAnswer.visible()
            }
            InterviewRoomState.TARGET_JOB_PARSE_FAILED -> {
                binding.layoutPrepare.visible()
                binding.tvTargetJobTitle.text = "重新上传"
            }
            InterviewRoomState.REPORT_GENERATING,
            InterviewRoomState.FINISHED,
            InterviewRoomState.CANCELED -> {
                binding.layoutFinish.visible()
            }
            else -> {
                binding.layoutPrepare.visible()
            }
        }
    }

    private fun updateSessionStatus(sessionStatus: String?) {
        binding.tvTitle.text = if (sessionStatus == SESSION_STATUS_PREPARING) {
            "面试准备中"
        } else {
            "面试中"
        }
    }

    private fun toggleAnswerRecording() {
        if (answerRecording) {
            answerRecording = false
            // 点击结束回答后等待 ASR 返回最终文本，计时先停，文本由回调发送。
            mRealVM.stopTiming()
            finishVoiceRecognizing(canceled = false)
            return
        }
        if (startVoiceRecognizing()) {
            mRealVM.startAnswer()
            answerRecording = true
            // 面试作答是点击开始，不走聊天页的按住录音浮层。
            updateVoiceActionRecording(true)
            mRealVM.startTiming()
        }
    }

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
        return runCatching {
            ensureSpeechTranscriber().start()
        }.getOrElse { throwable ->
            Log.d(TAG, "start voice recognizing failed", throwable)
            showMessage("语音输入启动失败")
            false
        }
    }

    private fun finishVoiceRecognizing(canceled: Boolean) {
        if (canceled) {
            shouldSendVoiceResult = false
            pendingVoiceText = null
            speechTranscriber?.cancel()
            resetVoiceRecognizingState()
            return
        }
        shouldSendVoiceResult = true
        if (sendVoiceMessage()) {
            speechTranscriber?.cancel()
        } else {
            speechTranscriber?.stop()
        }
        resetVoiceRecognizingState()
    }

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
                        sendVoiceMessage()
                    }
                }

                override fun onError(message: String) {
                    if (isDestroyed) return
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage(message)
                    } else if (!shouldSendVoiceResult) {
                        showMessage(message)
                    }
                    shouldSendVoiceResult = false
                    resetVoiceRecognizingState()
                }

                override fun onStopped() {
                    if (isDestroyed) return
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage("未识别到语音内容")
                    }
                    shouldSendVoiceResult = false
                    resetVoiceRecognizingState()
                }
            },
        ).also { speechTranscriber = it }
    }

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
                override fun onError(message: String) {
                    Log.d(TAG, "tts play failed: $message")
                    showMessage(message)
                }
            },
        ).also { ttsPlayer = it }
    }

    private fun sendVoiceMessage(): Boolean {
        val text = pendingVoiceText?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        pendingVoiceText = null
        shouldSendVoiceResult = false
        mRealVM.submitAnswer(text)
        return true
    }

    private fun resetVoiceRecognizingState() {
        answerRecording = false
        updateVoiceActionRecording(false)
        mRealVM.stopTiming()
    }

    private fun updateVoiceActionRecording(recording: Boolean) {
        binding.ivVoiceIcon.setImageResource(
            if (recording) R.drawable.img_interview_voice_icon_recording
            else R.drawable.img_interview_voice_icon_start
        )
        if (recording) {
            binding.ivVoiceBadge.startWaveAnimation()
        } else {
            binding.ivVoiceBadge.stopWaveAnimation()
        }
    }

    fun showFinishDialog() {
        val dialog = InterviewFinishDialog(this).showDialog()
        dialog.listener = { finish() }
    }

    fun showTargetJobDialog() {
        InterviewJobDialog(this).showDialog()
    }

    private fun requestAlbumPermission() {
        requestImages2VideosPermissions {
            if (it) openAlbum()
        }
    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            mRealVM.uploadTarget(Matisse.obtainResult(data).orEmpty())
        }
    }

    override fun onDestroy() {
        binding.ivVoiceBadge.stopWaveAnimation()
        speechTranscriber?.release()
        speechTranscriber = null
        ttsPlayer?.release()
        ttsPlayer = null
        super.onDestroy()
    }
}
