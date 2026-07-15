package com.shiyuan.fastclaw.feature.clawagent

import android.Manifest
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.databinding.ViewDataBinding
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.viewmodel.BaseLayoutViewModel
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.common.permissions.PermissionsUtils
import com.shiyuan.fastclaw.common.permissions.requestRxPermissions
import com.shiyuan.fastclaw.common.speech.DashFunAsrSpeechTranscriber
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.databinding.FragmentClawAgentMainBinding
import com.shiyuan.fastclaw.feature.clawagent.dialog.VoiceRecordingDialog
import com.shiyuan.fastclaw.router.AppNavigator

/**
 * @desc：聊天基类
 * @author：CaoWei
 * @date：2026/6/17 18:11
 */
open class BaseClawAgentFragment<VB : FragmentClawAgentMainBinding, VM : BaseLayoutViewModel> : BaseViewModelFragment<VB, VM>() {

    /** 语音识别 SDK 封装实例，按需创建并在销毁时释放。 */
    private var speechTranscriber: DashFunAsrSpeechTranscriber? = null

    /** 松手后是否允许发送当前识别结果。 */
    private var shouldSendVoiceResult = false

    /** 语音识别中的临时文本，优先发送 final，必要时兜底发送 partial。 */
    private var pendingVoiceText: String? = null

    /** 当前按住说话是否处于上滑取消态。 */
    private var voiceCanceling = false

    /** 按下时的屏幕 Y 坐标，用于计算上滑取消距离。 */
    private var voiceDownRawY = 0f

    /** 本次触摸是否已经成功进入录音识别流程。 */
    private var voiceTouchRecording = false

    /** 语音识别开始时间，用于过滤过短按压。 */
    private var voiceStartElapsedMs = 0L

    /** 按住说话期间展示的录音底部 Dialog。 */
    private var voiceRecordingDialog: VoiceRecordingDialog? = null

    /** 软键盘展示时不显示“滚动到底部”按钮。 */
    protected var isScrollBottomKeyboardShowing = false

    /** 记录底部按钮当前是否处于展示态，避免重复启动展示/隐藏动画。 */
    private var isScrollBottomButtonShown = false

    /** 自动滚到底部期间临时屏蔽按钮展示，避免按钮闪烁。 */
    private var isScrollBottomButtonSuppressed = false

    /** 自动滚动结束后恢复底部按钮状态判断的延迟任务。 */
    private val resumeScrollBottomButtonRunnable = Runnable {
        isScrollBottomButtonSuppressed = false
        updateScrollBottomButton()
    }

    companion object {
        /** 语音输入最短有效按压时长。 */
        private const val MIN_VOICE_INPUT_DURATION_MS = 1000L

        /** 按住说话上滑取消的触发距离。 */
        private val VOICE_CANCEL_DISTANCE = 40.dp

        /** 消息列表滚到底部前的默认延迟，等待列表布局完成。 */
        private const val MESSAGE_SCROLL_DELAY = 100L

        /** 消息列表平滑滚动速度。 */
        private const val MESSAGE_SCROLL_SPEED = 0.08f

        /** 底部按钮展示/隐藏时的纵向位移。 */
        private const val SCROLL_BOTTOM_BUTTON_ANIM_OFFSET = 30

        /** 底部按钮展示/隐藏动画时长。 */
        private const val SCROLL_BOTTOM_BUTTON_ANIM_DURATION = 50L

        /** 自动滚底过程中，最后一次滚动后继续屏蔽按钮的兜底时长。 */
        private const val SCROLL_BOTTOM_AUTO_SUPPRESS_DURATION = 300L
    }

    /**
     * 业务侧追踪类型：onTrackingEvent 内部根据该值统一调用 EventManager.onAppTrack，
     * 避免埋点 code 散落在各处回调中。
     */
    protected enum class TrackingType {
        /** 首次点击上传附件简历 */
        UPLOAD_RESUME,
        /** 简历解析失败后点击重新上传 */
        UPLOAD_RESUME_RETRY,
        /** 确认绑定历史简历 */
        RESUME_CONFIRM,
        /** 拒绝绑定历史简历 */
        RESUME_DENY,
    }

    /**
     * 语音识别出文本后由子类发送到对应业务通道。
     */
    protected open fun onVoiceTextRecognized(text: String) = Unit

    /**
     * 语音触摸/识别状态复位后通知子类刷新输入框 UI。
     */
    protected open fun onVoiceRecognizingStateReset() = Unit

    /**
     * 根据列表是否离开底部、键盘状态和屏蔽状态，刷新“滚动到底部”按钮可见性。
     */
    protected fun updateScrollBottomButton() {
        if (isScrollBottomButtonSuppressed) {
            suppressScrollBottomButton()
            return
        }
        if ((binding.rvMsg.adapter?.itemCount ?: 0) > 0 && binding.rvMsg.canScrollVertically(1) && !isScrollBottomKeyboardShowing) {
            showScrollBottomButton()
        } else {
            hideScrollBottomButton()
        }
    }

    /**
     * 从下往上展示“滚动到底部”按钮；已展示时不重复播放动画。
     */
    protected fun showScrollBottomButton() {
        if (isScrollBottomButtonShown) return
        isScrollBottomButtonShown = true
        binding.ivScrollBottom.animate().cancel()
        if (binding.ivScrollBottom.visibility != View.VISIBLE) {
            binding.ivScrollBottom.translationY = SCROLL_BOTTOM_BUTTON_ANIM_OFFSET.dp.toFloat()
            binding.ivScrollBottom.visible()
        }
        binding.ivScrollBottom.animate()
            .translationY(0f)
            .setDuration(SCROLL_BOTTOM_BUTTON_ANIM_DURATION)
            .withEndAction(null)
            .start()
    }

    /**
     * 从上往下隐藏“滚动到底部”按钮；隐藏中再次触发时不重启动画。
     */
    protected fun hideScrollBottomButton() {
        if (!isScrollBottomButtonShown) {
            if (binding.ivScrollBottom.visibility != View.VISIBLE) {
                binding.ivScrollBottom.translationY = 0f
            }
            return
        }
        isScrollBottomButtonShown = false
        binding.ivScrollBottom.animate().cancel()
        if (binding.ivScrollBottom.visibility != View.VISIBLE) {
            binding.ivScrollBottom.translationY = 0f
            return
        }
        binding.ivScrollBottom.animate()
            .translationY(SCROLL_BOTTOM_BUTTON_ANIM_OFFSET.dp.toFloat())
            .setDuration(SCROLL_BOTTOM_BUTTON_ANIM_DURATION)
            .withEndAction {
                if (!isScrollBottomButtonShown) {
                    binding.ivScrollBottom.gone()
                    binding.ivScrollBottom.translationY = 0f
                }
            }
            .start()
    }

    /**
     * 自动滚动期间屏蔽底部按钮展示，并在滚动结束后延迟恢复状态判断。
     */
    protected fun suppressScrollBottomButton(delayTime: Long = 0L) {
        isScrollBottomButtonSuppressed = true
        hideScrollBottomButton()
        binding.rvMsg.removeCallbacks(resumeScrollBottomButtonRunnable)
        binding.rvMsg.postDelayed(resumeScrollBottomButtonRunnable, delayTime + SCROLL_BOTTOM_AUTO_SUPPRESS_DURATION)
    }

    /**
     * 将消息列表滚动到底部：首次加载可直接贴底，后续新增消息使用平滑滚动。
     */
    protected fun scrollToBottom(hadObservedAnimation: Boolean, delayTime: Long = MESSAGE_SCROLL_DELAY) {
        suppressScrollBottomButton(delayTime)
        if (hadObservedAnimation) {
            binding.rvMsg.stopScroll()
            RecyclerViewScrollHelper.smoothScrollToBottom(binding.rvMsg, delayTime, MESSAGE_SCROLL_SPEED)
        } else {
            RecyclerViewScrollHelper.scrollToBottom(binding.rvMsg, delayTime)
        }
    }

    /**
     * 处理“按住说话”区域的触摸事件。
     *
     * 子类只需要把语音按钮的 onTouch 交给这个方法，基类会负责开始识别、上滑取消和松开发送。
     */
    protected fun handleVoicePressTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (AppNavigator.checkLoginIntercepted(false)) return true
                voiceDownRawY = event.rawY
                voiceCanceling = false
                voiceTouchRecording = startVoiceRecognizing()
                if (voiceTouchRecording) {
                    // 识别启动成功后再展示录音浮层，避免权限申请或启动失败时出现假录音态。
                    voiceRecordingDialog = VoiceRecordingDialog.show(parentFragmentManager)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!voiceTouchRecording) return true
                updateVoiceCancelState(event.rawY)
                voiceRecordingDialog?.updateCanceling(voiceCanceling)
            }
            MotionEvent.ACTION_UP -> {
                if (!voiceTouchRecording) return true
                hideVoiceRecordingDialog()
                finishVoiceRecognizing(canceled = voiceCanceling)
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!voiceTouchRecording) return true
                hideVoiceRecordingDialog()
                finishVoiceRecognizing(canceled = true)
            }
        }
        return true
    }

    /**
     * Fragment 销毁时释放语音 SDK 和录音资源。
     */
    protected fun releaseVoiceRecognizing() {
        speechTranscriber?.release()
        speechTranscriber = null
        hideVoiceRecordingDialog()
    }

    /**
     * 根据手指 Y 坐标更新上滑取消态。
     */
    private fun updateVoiceCancelState(rawY: Float) {
        // 以按下点为基准，上滑超过阈值进入取消态；滑回阈值内恢复发送态。
        voiceCanceling = voiceDownRawY - rawY > VOICE_CANCEL_DISTANCE
    }

    /**
     * 隐藏录音 Dialog 并清空当前引用。
     */
    private fun hideVoiceRecordingDialog() {
        voiceRecordingDialog?.hideRecording()
        voiceRecordingDialog = null
    }

    /**
     * 结束本次语音识别，根据取消态、时长和识别结果决定是否发送。
     */
    private fun finishVoiceRecognizing(canceled: Boolean) {
        if (canceled) {
            shouldSendVoiceResult = false
            pendingVoiceText = null
            voiceStartElapsedMs = 0L
            speechTranscriber?.cancel()
            resetVoiceTouchState()
            return
        }
        if (SystemClock.elapsedRealtime() - voiceStartElapsedMs < MIN_VOICE_INPUT_DURATION_MS) {
            shouldSendVoiceResult = false
            pendingVoiceText = null
            voiceStartElapsedMs = 0L
            speechTranscriber?.cancel()
            resetVoiceTouchState()
            return
        }
        // 松开时如果已有 partial 结果先发送；否则等待最终结果或 stopped 回调再兜底处理。
        shouldSendVoiceResult = true
        if (sendVoiceMessage()) {
            speechTranscriber?.cancel()
        } else {
            speechTranscriber?.stop()
        }
        resetVoiceTouchState()
    }

    /**
     * 检查录音权限并启动语音识别。
     */
    private fun startVoiceRecognizing(): Boolean {
        if (!PermissionsUtils.checkPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
            requestRxPermissions(getString(R.string.toast_open_mike_permission), Manifest.permission.RECORD_AUDIO) { granted ->
                if (!granted) showMessage(getString(R.string.toast_open_mike_permission))
            }
            return false
        }
        pendingVoiceText = null
        shouldSendVoiceResult = false
        return runCatching {
            val started = ensureSpeechTranscriber().start()
            voiceStartElapsedMs = if (started) SystemClock.elapsedRealtime() else 0L
            started
        }.getOrElse { throwable ->
            voiceStartElapsedMs = 0L
            Log.d("startVoiceRecognizing", "start voice recognizing failed", throwable)
            showMessage("语音输入启动失败")
            false
        }
    }

    /**
     * 懒创建语音识别器，并把识别回调同步到当前页面状态。
     */
    private fun ensureSpeechTranscriber(): DashFunAsrSpeechTranscriber {
        return speechTranscriber ?: DashFunAsrSpeechTranscriber(
            context = requireContext().applicationContext,
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

                override fun onVolumeChanged(rmsDb: Float) {
                    voiceRecordingDialog?.updateVolume(rmsDb)
                }

                override fun onError(message: String) {
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage(message)
                    } else if (!shouldSendVoiceResult) {
                        showMessage(message)
                    }
                    shouldSendVoiceResult = false
                    voiceStartElapsedMs = 0L
                }

                override fun onStopped() {
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage("未识别到语音内容")
                    }
                    shouldSendVoiceResult = false
                    voiceStartElapsedMs = 0L
                }
            },
        ).also { speechTranscriber = it }
    }

    /**
     * 将已识别文本交给子类发送，发送成功后清空本次识别缓存。
     */
    private fun sendVoiceMessage(): Boolean {
        val text = pendingVoiceText?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        pendingVoiceText = null
        shouldSendVoiceResult = false
        voiceStartElapsedMs = 0L
        onVoiceTextRecognized(text)
        return true
    }

    /**
     * 重置触摸和取消态，并通知子类刷新输入区 UI。
     */
    private fun resetVoiceTouchState() {
        voiceTouchRecording = false
        voiceCanceling = false
        voiceDownRawY = 0f
        onVoiceRecognizingStateReset()
    }

    override fun onDestroyView() {
        binding.rvMsg.removeCallbacks(resumeScrollBottomButtonRunnable)
        releaseVoiceRecognizing()
        super.onDestroyView()
    }

    /**
     * 业务埋点统一出口：所有 EventManager.onAppTrack 调用都收拢到本方法中，
     * 避免在各类点击/事件回调中散落埋点 code。业务回调只需传入对应 TrackingType，
     * 即可由本方法完成 code → EventManager 调用的映射。
     *
     * - [trackingType]：UI 层的离散动作（点击上传/重传/确认简历/否认简历）
     * - [payload] / [clawAgentEvent]：与原有 CoreQuestionPayloadBean / ClawAgentEvent 联动触发的埋点保持不变
     */
    protected fun onTrackingEvent(
        trackingType: TrackingType? = null,
        payload: CoreQuestionPayloadBean? = null,
        clawAgentEvent: Events.ClawAgentEvent? = null,
    ) {
        when (trackingType) {
            TrackingType.UPLOAD_RESUME ->
                EventManager.onAppTrack(AppEvent.KLX_C_UPLOAD_RESUME, mutableMapOf("klx_pipe_source" to "APP"))
            TrackingType.UPLOAD_RESUME_RETRY ->
                EventManager.onAppTrack(AppEvent.KLX_CAPP_UPLOAD_FAILED_RE_RESUME)
            TrackingType.RESUME_CONFIRM ->
                EventManager.onAppTrack(AppEvent.KLX_CAPP_CONFIRM_RESUME)
            TrackingType.RESUME_DENY ->
                EventManager.onAppTrack(AppEvent.KLX_CAPP_DENY_RESUME)
            null -> Unit
        }

        payload?.let {
            if (it.attributeGroupCode == "jobStatus") {
                EventManager.onAppTrack(AppEvent.KLX_C_JOBSEEKING_STATUS_COMPLETED, mutableMapOf("klx_pipe_source" to "APP"))
            }
        }

        clawAgentEvent?.let {
            if (it.type == AppEvent.KLX_C_EXPECTED_JOB_COMPLETED) {
                EventManager.onAppTrack(AppEvent.KLX_C_EXPECTED_JOB_COMPLETED, mutableMapOf("klx_pipe_source" to "APP"))
            } else if (it.type == AppEvent.KLX_C_EXPECTED_CITY_COMPLETED) {
                EventManager.onAppTrack(AppEvent.KLX_C_EXPECTED_CITY_COMPLETED, mutableMapOf("klx_pipe_source" to "APP"))
            } else if (it.type == AppEvent.KLX_C_EXPECTED_SALARY_COMPLETED) {
                EventManager.onAppTrack(AppEvent.KLX_C_EXPECTED_SALARY_COMPLETED, mutableMapOf("klx_pipe_source" to "APP"))
            }
        }
    }
}
