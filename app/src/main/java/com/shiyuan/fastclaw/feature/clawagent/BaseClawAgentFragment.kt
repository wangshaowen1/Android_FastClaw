package com.shiyuan.fastclaw.feature.clawagent

import android.Manifest
import android.util.Log
import android.view.MotionEvent
import androidx.databinding.ViewDataBinding
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.viewmodel.BaseLayoutViewModel
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.permissions.PermissionsUtils
import com.shiyuan.fastclaw.common.permissions.requestRxPermissions
import com.shiyuan.fastclaw.common.speech.DashFunAsrSpeechTranscriber
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_CANCEL
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_DOWN
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_MOVE
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_UP
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_VOLUME
import com.shiyuan.fastclaw.databinding.FragmentClawAgentMainBinding
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.VoiceRecordingView.Companion.VOICE_CANCEL_DISTANCE

/**
 * @desc：聊天基类
 * @author：CaoWei
 * @date：2026/6/17 18:11
 */
open class BaseClawAgentFragment<VM : BaseLayoutViewModel> : BaseViewModelFragment<FragmentClawAgentMainBinding, VM>() {

    private var speechTranscriber: DashFunAsrSpeechTranscriber? = null
    private var shouldSendVoiceResult = false
    private var pendingVoiceText: String? = null
    private var voiceCanceling = false
    private var voiceDownRawY = 0f
    private var voiceTouchRecording = false

    /**
     * 语音识别出文本后由子类发送到对应业务通道。
     */
    protected open fun onVoiceTextRecognized(text: String) = Unit

    /**
     * 语音触摸/识别状态复位后通知子类刷新输入框 UI。
     */
    protected open fun onVoiceRecognizingStateReset() = Unit

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
                    FlowEventBus.postSticky(Events.ClawVoiceEvent(MSG_ACTION_DOWN, event))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!voiceTouchRecording) return true
                updateVoiceCancelState(event.rawY)
                FlowEventBus.postSticky(Events.ClawVoiceEvent(MSG_ACTION_MOVE, event))
            }
            MotionEvent.ACTION_UP -> {
                if (!voiceTouchRecording) return true
                FlowEventBus.postSticky(Events.ClawVoiceEvent(MSG_ACTION_UP, event))
                finishVoiceRecognizing(canceled = voiceCanceling)
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!voiceTouchRecording) return true
                FlowEventBus.postSticky(Events.ClawVoiceEvent(MSG_ACTION_CANCEL, event))
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
    }

    private fun updateVoiceCancelState(rawY: Float) {
        // 以按下点为基准，上滑超过阈值进入取消态；滑回阈值内恢复发送态。
        voiceCanceling = voiceDownRawY - rawY > VOICE_CANCEL_DISTANCE
    }

    private fun finishVoiceRecognizing(canceled: Boolean) {
        if (canceled) {
            shouldSendVoiceResult = false
            pendingVoiceText = null
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
            ensureSpeechTranscriber().start()
        }.getOrElse { throwable ->
            Log.d("startVoiceRecognizing", "start voice recognizing failed", throwable)
            showMessage("语音输入启动失败")
            false
        }
    }

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
                    FlowEventBus.postSticky(Events.ClawVoiceEvent(MSG_ACTION_VOLUME, rmsDb))
                }

                override fun onError(message: String) {
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage(message)
                    } else if (!shouldSendVoiceResult) {
                        showMessage(message)
                    }
                    shouldSendVoiceResult = false
                }

                override fun onStopped() {
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage("未识别到语音内容")
                    }
                    shouldSendVoiceResult = false
                }
            },
        ).also { speechTranscriber = it }
    }

    private fun sendVoiceMessage(): Boolean {
        val text = pendingVoiceText?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        pendingVoiceText = null
        shouldSendVoiceResult = false
        onVoiceTextRecognized(text)
        return true
    }

    private fun resetVoiceTouchState() {
        voiceTouchRecording = false
        voiceCanceling = false
        voiceDownRawY = 0f
        onVoiceRecognizingStateReset()
    }

    override fun onDestroyView() {
        releaseVoiceRecognizing()
        super.onDestroyView()
    }
}
