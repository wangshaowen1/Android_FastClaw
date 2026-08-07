package com.shiyuan.fastclaw.feature.clawagent

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.databinding.ViewDataBinding
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.showAllowingStateLoss
import com.nhsd.basic.viewmodel.BaseLayoutViewModel
import com.nhsd.basic.viewmodel.UiState
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.common.permissions.PermissionsUtils
import com.shiyuan.fastclaw.common.permissions.requestRxPermissions
import com.shiyuan.fastclaw.common.speech.DashFunAsrSpeechTranscriber
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.showKeyboard
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.AppEventParam
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.databinding.FragmentClawAgentMainBinding
import com.shiyuan.fastclaw.feature.clawagent.dialog.VoiceRecordingDialog
import com.shiyuan.fastclaw.feature.login.dialog.LoginNewLoadingDialog
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.CommonLoadingDialog

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

    /** et_send 是否已经长按进入语音录制。 */
    private var isEtSendVoicePressing = false

    /** et_send 按下事件副本，长按触发录音时补发 ACTION_DOWN。 */
    private var etSendVoiceDownEvent: MotionEvent? = null

    /** et_send 长按启动语音录制的延迟任务。 */
    private var etSendVoiceLongPressRunnable: Runnable? = null

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
        /** 当前页面日志标识。 */
        const val TAG = "ClawAgentMainFragment"

        /** 记忆卡片等待时长 */
        const val MEMORY_CARD_WAIT_TIME = 5000L

        /** 距顶部多少条时开始预取更早历史 */
        const val PREFETCH_THRESHOLD = 5

        /** 键盘收起时底部输入栏的基础底部间距，单位 dp。 */
        const val KEYBOARD_BASE_BOTTOM_OFFSET = 95

        /** 首页恢复后补拉消息的轮询间隔，单位毫秒。 */
        const val RESUME_EVENT_POLL_INTERVAL = 10000L

        /** 首页恢复后补拉消息的最长轮询时长，单位毫秒。 */
        const val RESUME_EVENT_POLL_TIMEOUT = 60000L

        /** 语音输入最短有效按压时长。 */
        private const val MIN_VOICE_INPUT_DURATION_MS = 1000L

        /** 录音开始时的轻震时长。 */
        private const val VOICE_START_VIBRATE_MS = 35L

        /** 录音开始时的轻震强度。 */
        private const val VOICE_START_VIBRATE_AMPLITUDE = 100

        /** 按住说话上滑取消的触发距离。 */
        private val VOICE_CANCEL_DISTANCE = 40.dp

        /** 消息列表滚到底部前的默认延迟，0ms 可减少新卡片出现瞬间的停顿感。 */
        private const val MESSAGE_SCROLL_DELAY = 0L

        /** 消息列表平滑滚动速度。 */
        private const val MESSAGE_SCROLL_SPEED = 0.10f

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
            // 自动滚到底部期间强制隐藏按钮，避免新增消息时按钮闪一下又消失。
            suppressScrollBottomButton()
            return
        }
        // 列表有内容、还能继续向下滚、且键盘未展示时，说明用户已经离开底部，需要露出回到底部按钮。
        val itemCount = binding.rvMsg.adapter?.itemCount ?: 0
        if (itemCount > 1 && binding.rvMsg.canScrollVertically(1) && !isScrollBottomKeyboardShowing) {
            showScrollBottomButton()
        } else {
            hideScrollBottomButton()
        }
    }

    /**
     * 从下往上展示“滚动到底部”按钮；已展示时不重复播放动画。
     */
    protected fun showScrollBottomButton() {
        // 已经是展示态就不重复开启动画，避免滚动监听频繁回调造成抖动。
        if (isScrollBottomButtonShown) return
        isScrollBottomButtonShown = true
        binding.ivScrollBottom.animate().cancel()
        if (binding.ivScrollBottom.visibility != View.VISIBLE) {
            // 首次展示前先放到下方偏移位置，后续动画再滑回原位。
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
                // 已经隐藏时兜底重置位移，避免下次显示从错误位置开始。
                binding.ivScrollBottom.translationY = 0f
            }
            return
        }
        isScrollBottomButtonShown = false
        binding.ivScrollBottom.animate().cancel()
        if (binding.ivScrollBottom.visibility != View.VISIBLE) {
            // View 已不可见时只重置位移，不再播放隐藏动画。
            binding.ivScrollBottom.translationY = 0f
            return
        }
        binding.ivScrollBottom.animate()
            .translationY(SCROLL_BOTTOM_BUTTON_ANIM_OFFSET.dp.toFloat())
            .setDuration(SCROLL_BOTTOM_BUTTON_ANIM_DURATION)
            .withEndAction {
                if (!isScrollBottomButtonShown) {
                    // 动画结束时再次确认状态，避免显示动画中途切回展示态后被误隐藏。
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
                // 录音入口需要先做登录拦截，避免未登录时启动语音 SDK。
                if (AppNavigator.checkLoginIntercepted(false,loginSource =AppEventParam.XIAOXIA_CHAT)) return true
                // 记录按下位置，后续 MOVE 用它判断是否上滑取消。
                voiceDownRawY = event.rawY
                voiceCanceling = false
                // 只有 SDK 启动成功才进入录音触摸态。
                voiceTouchRecording = startVoiceRecognizing()
                if (voiceTouchRecording) {
                    vibrateVoiceStart()
                    // 识别启动成功后再展示录音浮层，避免权限申请或启动失败时出现假录音态。
                    voiceRecordingDialog = VoiceRecordingDialog.show(parentFragmentManager)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!voiceTouchRecording) return true
                // 手指移动时实时更新“上滑取消/松开发送”的状态。
                updateVoiceCancelState(event.rawY)
                voiceRecordingDialog?.updateCanceling(voiceCanceling)
            }
            MotionEvent.ACTION_UP -> {
                if (!voiceTouchRecording) return true
                // 正常松手：关闭录音浮层，并按当前取消态决定发送或取消。
                hideVoiceRecordingDialog()
                finishVoiceRecognizing(canceled = voiceCanceling)
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!voiceTouchRecording) return true
                // 系统取消触摸时强制取消本次录音，避免误发送。
                hideVoiceRecordingDialog()
                finishVoiceRecognizing(canceled = true)
            }
        }
        return true
    }

    /** et_send 短按保留输入能力；长按开始录音后，移动和松手交给语音逻辑处理。 */
    protected fun handleEtSendVoiceTouch(event: MotionEvent): Boolean {
        // 输入框已有内容时不接管按下事件，保留 EditText 原生点击、选中文本和键盘行为。
        if (binding.etSend.text?.isNotBlank() == true || isScrollBottomKeyboardShowing) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 新一轮按下前先清掉上一次可能残留的延迟任务和事件副本。
                clearEtSendVoicePress()
                // 保存 DOWN 事件，长按成立后补发给语音逻辑作为录音起点。
                etSendVoiceDownEvent = MotionEvent.obtain(event)
                isEtSendVoicePressing = false
                etSendVoiceLongPressRunnable = Runnable {
                    isEtSendVoicePressing = true
                    // 长按录音开始后输入框不再处理文本输入，先收起键盘并清除焦点。
                    hideKeyboard(binding.etSend)
                    binding.etSend.clearFocus()
                    etSendVoiceDownEvent?.let { downEvent ->
                        // 复用按住说话逻辑，保证 et_send 长按和语音按钮行为一致。
                        handleVoicePressTouch(downEvent)
                    }
                }.also {
                    // 使用系统长按时长，避免和普通点击抢事件。
                    binding.etSend.postDelayed(it, ViewConfiguration.getLongPressTimeout().toLong())
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> if (isEtSendVoicePressing) {
                // 长按已经进入录音后，移动事件交给语音逻辑处理上滑取消。
                return handleVoicePressTouch(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 松手/取消时立即清理延迟任务，短按不会误触发录音。
                clearEtSendVoicePress()
                if (isEtSendVoicePressing) {
                    isEtSendVoicePressing = false
                    // 已经进入录音时，松手事件交给语音逻辑完成发送或取消。
                    return handleVoicePressTouch(event)
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    // 未进入录音说明是普通点击，恢复输入框焦点并拉起键盘。
                    binding.etSend.requestFocus()
                    showKeyboard(binding.etSend)
                }
                return true
            }
        }
        return true
    }

    /** 录音真正开始时轻震一次，给用户一个明确的按住反馈。 */
    private fun vibrateVoiceStart() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        VOICE_START_VIBRATE_MS,
                        VOICE_START_VIBRATE_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VOICE_START_VIBRATE_MS)
            }
        }
    }

    /** 清理 et_send 长按录音的延迟任务和按下事件，避免松手后状态残留。 */
    private fun clearEtSendVoicePress() {
        etSendVoiceLongPressRunnable?.let { binding.etSend.removeCallbacks(it) }
        etSendVoiceLongPressRunnable = null
        etSendVoiceDownEvent?.recycle()
        etSendVoiceDownEvent = null
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
            // 用户上滑取消时，不允许后续 final/stopped 回调再触发发送。
            shouldSendVoiceResult = false
            pendingVoiceText = null
            voiceStartElapsedMs = 0L
            speechTranscriber?.cancel()
            resetVoiceTouchState()
            return
        }
        if (SystemClock.elapsedRealtime() - voiceStartElapsedMs < MIN_VOICE_INPUT_DURATION_MS) {
            // 录音时间过短按取消处理，避免误触产生空消息或无效请求。
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
            // 已经拿到可发送文本时直接取消识别，避免 final 回调重复发送。
            speechTranscriber?.cancel()
        } else {
            // 暂无文本时停止识别，等待 SDK 返回 final 或 stopped 再做兜底。
            speechTranscriber?.stop()
        }
        resetVoiceTouchState()
    }

    /**
     * 检查录音权限并启动语音识别。
     */
    private fun startVoiceRecognizing(): Boolean {
        if (!PermissionsUtils.checkPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
            // 没有麦克风权限时先申请权限，本次触摸不进入录音态。
            requestRxPermissions(getString(R.string.toast_open_mike_permission), Manifest.permission.RECORD_AUDIO) { granted ->
                if (!granted) showMessage(getString(R.string.toast_open_mike_permission))
            }
            return false
        }
        // 开始新一轮识别前清空上一次缓存，避免旧文本被误发送。
        pendingVoiceText = null
        shouldSendVoiceResult = false
        return runCatching {
            val started = ensureSpeechTranscriber().start()
            // 只在启动成功时记录开始时间，用于松手时判断是否过短。
            voiceStartElapsedMs = if (started) SystemClock.elapsedRealtime() else 0L
            started
        }.getOrElse { throwable ->
            // 启动失败后清掉时间戳，下一次录音重新计算。
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
                    // partial 先缓存，用户松手时可先用它兜底发送。
                    pendingVoiceText = text
                }

                override fun onFinalResult(text: String) {
                    // final 覆盖 partial，保证最终发送优先使用更完整的识别结果。
                    pendingVoiceText = text
                    if (shouldSendVoiceResult) {
                        sendVoiceMessage()
                    }
                }

                override fun onVolumeChanged(rmsDb: Float) {
                    // 音量回调只驱动录音浮层动画，不参与发送状态判断。
                    voiceRecordingDialog?.updateVolume(rmsDb)
                }

                override fun onError(message: String) {
                    // 松手后报错时先尝试发送已缓存文本，没有文本再展示错误。
                    if (shouldSendVoiceResult && !sendVoiceMessage()) {
                        showMessage(message)
                    } else if (!shouldSendVoiceResult) {
                        showMessage(message)
                    }
                    shouldSendVoiceResult = false
                    voiceStartElapsedMs = 0L
                }

                override fun onStopped() {
                    // stop 后仍没有识别文本时提示用户，本次发送流程结束。
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
        // 空白识别结果不发送，交给 final/stopped 后续回调继续兜底。
        val text = pendingVoiceText?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        // 发送前清空本轮状态，避免 final/onStopped 再次命中同一段文本。
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
        clearEtSendVoicePress()
        isEtSendVoicePressing = false
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
    protected fun onTrackingEvent(trackingType: TrackingType? = null, payload: CoreQuestionPayloadBean? = null, clawAgentEvent: Events.ClawAgentEvent? = null, ) {
        when (trackingType) {
            TrackingType.UPLOAD_RESUME ->
                EventManager.onAppTrack(AppEvent.KLX_C_UPLOAD_RESUME, mutableMapOf(AppEventParam.KEY_PIPE_SOURCE to AppEventParam.VALUE_PIPE_SOURCE_APP))
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
                EventManager.onAppTrack(AppEvent.KLX_C_JOBSEEKING_STATUS_COMPLETED, mutableMapOf(AppEventParam.KEY_PIPE_SOURCE to AppEventParam.VALUE_PIPE_SOURCE_APP))
            }
        }

        clawAgentEvent?.let {
            if (it.type == AppEvent.KLX_C_EXPECTED_JOB_COMPLETED) {
                EventManager.onAppTrack(AppEvent.KLX_C_EXPECTED_JOB_COMPLETED, mutableMapOf(AppEventParam.KEY_PIPE_SOURCE to AppEventParam.VALUE_PIPE_SOURCE_APP))
            } else if (it.type == AppEvent.KLX_C_EXPECTED_CITY_COMPLETED) {
                EventManager.onAppTrack(AppEvent.KLX_C_EXPECTED_CITY_COMPLETED, mutableMapOf(AppEventParam.KEY_PIPE_SOURCE to AppEventParam.VALUE_PIPE_SOURCE_APP))
            } else if (it.type == AppEvent.KLX_C_EXPECTED_SALARY_COMPLETED) {
                EventManager.onAppTrack(AppEvent.KLX_C_EXPECTED_SALARY_COMPLETED, mutableMapOf(AppEventParam.KEY_PIPE_SOURCE to AppEventParam.VALUE_PIPE_SOURCE_APP))
            }
        }
    }


    private var commonLoadingDialog: CommonLoadingDialog? = null

    fun showLoading(text: String? = null) {
        if (commonLoadingDialog?.isAdded == true) return
        commonLoadingDialog = CommonLoadingDialog.newInstance(text).apply {
            show(this@BaseClawAgentFragment.childFragmentManager)
        }
    }


     fun hideLoading() {
        commonLoadingDialog?.dismissAllowingStateLoss()
        commonLoadingDialog = null
    }

}
