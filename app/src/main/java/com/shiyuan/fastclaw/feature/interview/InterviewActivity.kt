package com.shiyuan.fastclaw.feature.interview

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
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
import com.shiyuan.fastclaw.common.speech.DashFunAsrSpeechTranscriber
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.core.ext.getBundleValue
import com.shiyuan.fastclaw.databinding.ActivityInterviewBinding
import com.shiyuan.fastclaw.feature.settings.report.ReportActivity.Companion.REQUEST_CODE_CHOOSE
import com.shiyuan.fastclaw.router.AppNavigator

class InterviewActivity : BaseViewModelActivity<ActivityInterviewBinding, InterviewViewModel>() {

    private val entryMessageId by lazy { intent.getBundleValue(KEY_ENTRY_MESSAGE_ID, "") }
    private val entrySource by lazy { intent.getBundleValue(KEY_ENTRY_SOURCE, "") }
    private val resumeId by lazy { intent.getBundleValue(KEY_RESUME_ID, "") }
    private val msgAdapter by lazy { InterviewRoomMessageAdapter() }
    private var speechTranscriber: DashFunAsrSpeechTranscriber? = null
    private var shouldSendVoiceResult = false
    private var pendingVoiceText: String? = null
    private var answerRecording = false
    /** 用户主动发送后,强制把列表滚到最新一条(不受当前是否已在底部影响) */
    private var forceScrollToBottom = false

    companion object {
        private const val TAG = "InterviewActivity"
        private const val REQUEST_CODE_TARGET_JOB = 0x6101
        private const val KEY_ENTRY_MESSAGE_ID = "KEY_ENTRY_MESSAGE_ID"
        private const val KEY_ENTRY_SOURCE = "KEY_ENTRY_SOURCE"
        private const val KEY_RESUME_ID = "KEY_RESUME_ID"

        fun start(activity: Activity?, entryMessageId: String?, entrySource: String?, resumeId: String?) {
            activity ?: return
            activity.startActivity(
                Intent(activity, InterviewActivity::class.java).apply {
                    putExtra(KEY_ENTRY_MESSAGE_ID, entryMessageId)
                    putExtra(KEY_ENTRY_SOURCE, entrySource)
                    putExtra(KEY_RESUME_ID, resumeId)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeData()
        mRealVM.startInterview(entryMessageId, entrySource, resumeId)
    }

    private fun initView() {
        binding.rvMessage.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessage.adapter = msgAdapter
        binding.ivBack.setOnSingleClickListener { mRealVM.exitInterview() }
        binding.btnTargetJob.setOnSingleClickListener { requestAlbumPermission() }
        binding.btnRetryUpload.setOnSingleClickListener { requestAlbumPermission() }
        binding.layoutAnswer.setOnSingleClickListener { toggleAnswerRecording() }
        binding.tvVoicePress.setOnSingleClickListener { toggleAnswerRecording() }
    }

    private fun observeData() {
        mRealVM.messageList.observe(this) { list ->
            // 是否滚到最新：用户刚发送(forceScrollToBottom)强制滚动；否则仅在原本已在底部时
            // 跟随新消息/流式增量，避免打断向上浏览历史。判定须在 submitList 之前取(反映旧位置)。
            val scrollToLatest = forceScrollToBottom || !binding.rvMessage.canScrollVertically(1)
            forceScrollToBottom = false
            msgAdapter.submitList(list)
            if (scrollToLatest && list.isNotEmpty()) {
                // 跳转 + 补偿滚动距离，确保刚插入(尚未测量高度)的最后一条完整贴底
                RecyclerViewScrollHelper.scrollToBottom(binding.rvMessage, 100)
            }
        }
        mRealVM.roomState.observe(this) { updateRoomState(it) }
        mRealVM.answerActionText.observe(this) {
            binding.tvVoicePress.text = it
        }
        mRealVM.answerDurationText.observe(this) {
            binding.tvVoiceDuration.text = it
            if (it.isNotEmpty()) {
                binding.tvVoiceDuration.visible()
            } else {
                binding.tvVoiceDuration.gone()
            }
        }
        mRealVM.toastMessage.observe(this) {
            showMessage(it)
        }
        mRealVM.finishRoomEvent.observe(this) {
            finish()
        }
    }

    private fun updateRoomState(state: InterviewRoomState) {
        binding.layoutPrepare.gone()
        binding.layoutAnswer.gone()
        binding.layoutFinish.gone()
        binding.tvTargetJobTitle.text = "我有明确岗位"
        binding.tvTitle.text = when (state) {
            InterviewRoomState.STARTING -> "面试准备中"
            InterviewRoomState.PREPARING -> "面试准备中"
            InterviewRoomState.WAITING_TARGET_JOB -> "面试准备中"
            InterviewRoomState.TARGET_JOB_PARSING -> "面试准备中"
            InterviewRoomState.INTERVIEWING -> "面试中"
            InterviewRoomState.ANSWERING -> "面试中"
            InterviewRoomState.FINISHED -> "面试中"
            InterviewRoomState.ERROR -> "异常"
        }
        when (state) {
            InterviewRoomState.INTERVIEWING, InterviewRoomState.ANSWERING -> {
                binding.layoutAnswer.visible()
                binding.tvVoicePress.visible()
            }
            InterviewRoomState.WAITING_TARGET_JOB -> {
                binding.layoutPrepare.visible()
                binding.tvTargetJobTitle.text = "重新上传"
            }
            InterviewRoomState.TARGET_JOB_PARSING -> {
                binding.layoutPrepare.visible()
            }
            InterviewRoomState.STARTING, InterviewRoomState.PREPARING -> {
                binding.layoutPrepare.visible()
            }
            InterviewRoomState.FINISHED -> {
                binding.layoutFinish.visible()
            }
            else -> {
                binding.tvVoicePress.gone()
                binding.btnRetryUpload.gone()
            }
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
            answerRecording = true
            // 面试作答是点击开始，不走聊天页的按住录音浮层。
            updateVoiceActionRecording(true)
            mRealVM.startAnswer()
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
                    pendingVoiceText = text
                    if (shouldSendVoiceResult) {
                        sendVoiceMessage()
                    }
                }

                override fun onError(message: String) {
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage(message)
                    } else if (!shouldSendVoiceResult) {
                        showMessage(message)
                    }
                    shouldSendVoiceResult = false
                    resetVoiceRecognizingState()
                }

                override fun onStopped() {
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage("未识别到语音内容")
                    }
                    shouldSendVoiceResult = false
                    resetVoiceRecognizingState()
                }
            },
        ).also { speechTranscriber = it }
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
        binding.ivVoiceBadge.setImageResource(
            if (recording) R.drawable.img_interview_voice_badge_recording
            else R.drawable.img_interview_voice_badge_start
        )
        binding.ivVoiceIcon.setImageResource(
            if (recording) R.drawable.img_interview_voice_icon_recording
            else R.drawable.img_interview_voice_icon_start
        )
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
            mRealVM.uploadTargetJob(Matisse.obtainResult(data).orEmpty())
        }
    }

    override fun onDestroy() {
        speechTranscriber?.release()
        speechTranscriber = null
        super.onDestroy()
    }
}
