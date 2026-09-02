package com.shiyuan.fastclaw.feature.clawagent

import android.os.Bundle
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isNotEmpty
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.databinding.DialogVoiceRecordingBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 按住说话底部浮层 Dialog，负责录音展示、取消态和音量波形动画。
 */
class VoiceRecordingDialog : BaseBottomDialogFragment<DialogVoiceRecordingBinding>() {

    companion object {
        /** 录音 Dialog 的唯一 tag，避免重复创建多个实例。 */
        private const val TAG = "VoiceRecordingDialog"

        /** 波形条数量，需要和 WAVE_BASE_HEIGHTS 保持一致。 */
        private const val WAVE_BAR_COUNT = 25

        /** 录音浮层淡入淡出动画时长。 */
        private const val FADE_DURATION_MS = 240L

        /** 淡出结束后再 dismiss，避免动画被提前截断。 */
        private const val HIDE_DISMISS_DELAY = FADE_DURATION_MS

        /** 波形条基础高度，中间高、两侧低。 */
        private val WAVE_BASE_HEIGHTS = intArrayOf(
            6, 14, 12, 12, 16, 16, 20, 16, 16, 17, 17, 22, 33,
            22, 17, 17, 16, 16, 20, 16, 16, 12, 12, 14, 6
        )

        /**
         * 展示录音 Dialog。
         *
         * 已存在同 tag Dialog 时复用，避免连续按下或快速重入时叠多个浮层。
         */
        fun show(manager: FragmentManager): VoiceRecordingDialog {
            val dialog = manager.findFragmentByTag(TAG) as? VoiceRecordingDialog ?: VoiceRecordingDialog()
            if (!dialog.isAdded) {
                dialog.showNow(manager, TAG)
            }
            // showNow 后立即进入录音态，保证长按开始时浮层和波形同步出现。
            dialog.showRecording()
            return dialog
        }
    }

    /** 波形动画任务，Dialog 销毁或隐藏时取消。 */
    private var waveJob: Job? = null

    /** 当前平滑后的音量值，控制波形高度和透明度。 */
    private var waveVolume = 0.25f

    /** 波形动画相位，让每一帧的条形高度产生流动效果。 */
    private var wavePhase = 0f

    /** 当前是否处于上滑取消态。 */
    private var canceling = false

    /** 当前淡入淡出动画，切换显示状态前先取消旧动画。 */
    private var fadeAnimator: ViewPropertyAnimator? = null

    /** 隐藏动画播放完成后关闭 Dialog 的延迟任务。 */
    private val dismissRunnable = Runnable {
        dismissAllowingStateLoss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    /**
     * 配置 Dialog Window。
     *
     * 去掉系统默认进出场动画和阴影背景，让录音浮层像页面底部控件一样展示。
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            // 录音浮层由自身 alpha 控制显隐，不使用 Dialog 默认进出场动画。
            window.setWindowAnimations(0)
            // 去掉 Dialog 默认半透明遮罩，避免页面底部被额外压暗。
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply {
                dimAmount = 0f
            }
        }
        // 初始置透明，随后 fadeIn 控制淡入，避免 Dialog 创建瞬间闪一下。
        binding.root.alpha = 0f
        //ignoreNavigationBarPadding()
        setupWaveBars()
    }

    /**
     * 进入录音展示态。
     */
    fun showRecording() {
        // 重新进入录音态时取消待关闭任务，避免刚显示又被上一次 hide 的延迟任务关闭。
        binding.root.removeCallbacks(dismissRunnable)
        updateCanceling(canceling = false)
        startWaveAnimation()
        fadeIn()
    }

    /**
     * 更新上滑取消态。
     */
    fun updateCanceling(canceling: Boolean) {
        this.canceling = canceling
        // 上滑取消时切换背景、提示文案和波形颜色，给用户明确的取消反馈。
        val backgroundRes = if (canceling) R.drawable.bg_agent_voice_cancel else R.drawable.bg_agent_voice_recording
        binding.layoutContainer.setBackgroundResource(backgroundRes)
        binding.tvVoiceRecordTip.text = if (canceling) "松开取消" else "松开发送，上滑取消"
        binding.tvVoiceRecordTip.setTextColor(if (canceling) R.color.color_ff0000.getResColor() else R.color.color_ffffff.getResColor())
        binding.tvVoiceRecordTip.alpha = if (canceling) 0.6f else 1f
        val barBg = if (canceling) R.drawable.bg_agent_voice_bar_cancel else R.drawable.bg_agent_voice_bar
        for (index in 0 until binding.layoutVoiceWave.childCount) {
            binding.layoutVoiceWave.getChildAt(index).setBackgroundResource(barBg)
        }
    }

    /**
     * 根据识别 SDK 返回的音量刷新波形。
     */
    fun updateVolume(rmsDb: Float) {
        // SDK 可能返回 dB、0~1 或 0~10 的音量值，这里统一归一化到 0.08~1。
        val targetVolume = when {
            rmsDb < 0f -> ((rmsDb + 90f) / 45f)
            rmsDb <= 1f -> rmsDb
            rmsDb <= 10f -> rmsDb / 10f
            else -> rmsDb / 60f
        }.coerceIn(0.08f, 1f)
        // 音量升高快一点、降低慢一点，避免波形抖动过硬。
        val smoothFactor = if (targetVolume > waveVolume) 0.55f else 0.28f
        waveVolume += (targetVolume - waveVolume) * smoothFactor
        renderWave(wavePhase)
    }

    /**
     * 退出录音展示态。
     */
    fun hideRecording() {
        stopWaveAnimation()
        // 退出前把音量压回低位，淡出时波形不会停在高峰值。
        updateVolume(0f)
        canceling = false
        fadeOut()
        binding.root.removeCallbacks(dismissRunnable)
        // 等淡出动画完成后再关闭 Dialog，避免动画被 dismiss 截断。
        binding.root.postDelayed(dismissRunnable, HIDE_DISMISS_DELAY)
    }

    /**
     * 录音浮层需要覆盖到底部导航栏区域，覆盖基类默认添加的导航栏 padding。
     */
    private fun ignoreNavigationBarPadding() {
        binding.root.setPadding(
            binding.root.paddingLeft,
            binding.root.paddingTop,
            binding.root.paddingRight,
            0
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, 0)
            insets
        }
        binding.root.post {
            ViewCompat.requestApplyInsets(binding.root)
        }
    }

    /**
     * 初始化波形条，只创建一次，后续只更新高度和透明度。
     */
    private fun setupWaveBars() {
        if (binding.layoutVoiceWave.isNotEmpty()) return
        repeat(WAVE_BAR_COUNT) {
            val bar = View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_agent_voice_bar)
            }
            val params = LinearLayout.LayoutParams(2.dp, WAVE_BASE_HEIGHTS[it].dp).apply {
                if (it < WAVE_BAR_COUNT - 1) {
                    marginEnd = 6.dp
                }
            }
            binding.layoutVoiceWave.addView(bar, params)
        }
    }

    /**
     * 启动波形循环动画。
     */
    private fun startWaveAnimation() {
        waveJob?.cancel()
        waveJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                renderWave(wavePhase)
                // phase 持续递增，让静态基础高度产生流动感。
                wavePhase += 0.35f
                delay(48L)
            }
        }
    }

    /**
     * 停止波形循环动画。
     */
    private fun stopWaveAnimation() {
        waveJob?.cancel()
        waveJob = null
    }

    /**
     * 淡入显示录音浮层。
     */
    private fun fadeIn() {
        fadeAnimator?.cancel()
        // 每次展示都从透明开始，避免复用 Dialog 时保留上一次 alpha。
        binding.root.visible()
        binding.root.alpha = 0f
        fadeAnimator = binding.root.animate()
            .alpha(1f)
            .setDuration(FADE_DURATION_MS)
            .withEndAction { fadeAnimator = null }
        fadeAnimator?.start()
    }

    /**
     * 淡出隐藏录音浮层。
     */
    private fun fadeOut() {
        fadeAnimator?.cancel()
        fadeAnimator = binding.root.animate()
            .alpha(0f)
            .setDuration(FADE_DURATION_MS)
            .withEndAction {
                binding.root.gone()
                fadeAnimator = null
            }
        fadeAnimator?.start()
    }

    /**
     * 按当前音量和相位绘制波形条。
     */
    private fun renderWave(phase: Float) {
        val center = (binding.layoutVoiceWave.childCount - 1) / 2f
        for (index in 0 until binding.layoutVoiceWave.childCount) {
            // 距中心越近透明度越高，视觉上形成中间更亮的声波。
            val distance = kotlin.math.abs(index - center) / center
            // 每根柱子使用不同相位，避免所有柱子同时变高变低。
            val pulse = ((kotlin.math.sin(index * 0.85f + phase) + 1f) / 2f).coerceIn(0f, 1f)
            val baseHeight = WAVE_BASE_HEIGHTS.getOrElse(index) { 12 }.dp
            // 最终高度 = 基础高度 * 音量缩放 * 动态脉冲缩放。
            val volumeScale = 0.42f + waveVolume * 0.90f
            val pulseScale = 0.65f + pulse * 0.45f
            val height = (baseHeight * pulseScale * volumeScale).toInt().coerceAtLeast(3.dp)
            val alpha = (0.22f + waveVolume * 0.38f + (1f - distance).coerceIn(0f, 1f) * 0.4f).coerceIn(0.22f, 1f)
            binding.layoutVoiceWave.getChildAt(index).apply {
                this.alpha = alpha
            }.updateLayoutParams<LinearLayout.LayoutParams> {
                this.height = height
            }
        }
    }

    /**
     * 清理动画、延迟任务和协程，避免 Dialog 销毁后继续刷新 UI。
     */
    override fun onDestroyView() {
        binding.root.removeCallbacks(dismissRunnable)
        fadeAnimator?.cancel()
        fadeAnimator = null
        stopWaveAnimation()
        super.onDestroyView()
    }
}
