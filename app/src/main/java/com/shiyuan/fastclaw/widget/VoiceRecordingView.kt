package com.shiyuan.fastclaw.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.view.isNotEmpty

/**
 * 语音录制底部浮层：负责展示录音/取消状态，以及根据音量驱动波形动画。
 */
class VoiceRecordingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val WAVE_BAR_COUNT = 25
        private const val FADE_DURATION_MS = 240L

        val VOICE_CANCEL_DISTANCE = 40.dp
        private val WAVE_BASE_HEIGHTS = intArrayOf(
            6, 14, 12, 12, 16, 16, 20, 16, 16, 17, 17, 22, 33,
            22, 17, 17, 16, 16, 20, 16, 16, 12, 12, 14, 6
        )
    }

    private val contentView: View
    private val tipView: TextView
    private val waveLayout: LinearLayout
    private var waveJob: Job? = null
    private var waveVolume = 0.25f
    private var wavePhase = 0f
    private var canceling = false
    private var fadeAnimator: ViewPropertyAnimator? = null

    init {
        // 将语音浮层布局整体收口到自定义 View 内部，外部只负责显示状态切换。
        LayoutInflater.from(context).inflate(R.layout.view_voice_recording, this)
        contentView = findViewById(R.id.layout_voice_recording_content)
        tipView = findViewById(R.id.tv_voice_record_tip)
        waveLayout = findViewById(R.id.layout_voice_wave)
        isClickable = true
        isFocusable = true
        alpha = 0f
        setupWaveBars()
    }

    /**
     * 展示录音浮层，并按当前手势状态切换“录音中/松开取消”样式。
     */
    fun showRecording(canceling: Boolean = false) {
        updateCanceling(canceling)
        startWaveAnimation()
        fadeIn()
    }

    /**
     * 隐藏录音浮层，同时停止波形动画并复位状态。
     */
    fun hideRecording() {
        stopWaveAnimation()
        updateVolume(0f)
        this.canceling = false
        fadeOut()
    }

    /**
     * 更新是否处于上滑取消状态。
     */
    fun updateCanceling(canceling: Boolean) {
        this.canceling = canceling
        contentView.setBackgroundResource(if (canceling) R.drawable.bg_agent_voice_cancel else R.drawable.bg_agent_voice_recording)
        tipView.text = if (canceling) "松开取消" else "松开发送，上滑取消"
        tipView.setTextColor(if (canceling) R.color.color_ff0000.getResColor() else R.color.color_ffffff.getResColor())
        tipView.alpha = if (canceling) 0.6f else 1f
        val barBg = if (canceling) R.drawable.bg_agent_voice_bar_cancel else R.drawable.bg_agent_voice_bar
        for (index in 0 until waveLayout.childCount) {
            waveLayout.getChildAt(index).setBackgroundResource(barBg)
        }
    }

    /**
     * 接收外部录音音量，换算成 0.15~1 的波形振幅系数。
     */
    fun updateVolume(rmsDb: Float) {
        val targetVolume = when {
            rmsDb < 0f -> ((rmsDb + 90f) / 45f)
            rmsDb <= 1f -> rmsDb
            rmsDb <= 10f -> rmsDb / 10f
            else -> rmsDb / 60f
        }.coerceIn(0.08f, 1f)
        val smoothFactor = if (targetVolume > waveVolume) 0.55f else 0.28f
        waveVolume += (targetVolume - waveVolume) * smoothFactor
        if (isVisible) {
            renderWave(wavePhase)
        }
    }

    /**
     * 初始化中间的音频条纹，后续动画只更新高度和透明度。
     */
    private fun setupWaveBars() {
        if (waveLayout.isNotEmpty()) return
        repeat(WAVE_BAR_COUNT) {
            val bar = View(context).apply {
                setBackgroundResource(R.drawable.bg_agent_voice_bar)
            }
            val params = LinearLayout.LayoutParams(2.dp, WAVE_BASE_HEIGHTS[it].dp).apply {
                if (it < WAVE_BAR_COUNT - 1) {
                    marginEnd = 6.dp
                }
            }
            waveLayout.addView(bar, params)
        }
    }

    /**
     * 启动波形循环动画，绑定到当前 ViewTree 的 Lifecycle，避免页面销毁后继续刷新。
     */
    private fun startWaveAnimation() {
        waveJob?.cancel()
        waveJob = (findViewTreeLifecycleOwner()?.lifecycleScope ?: return).launch {
            while (isActive) {
                renderWave(wavePhase)
                wavePhase += 0.35f
                delay(48L)
            }
        }
    }

    /**
     * 停止波形动画。
     */
    private fun stopWaveAnimation() {
        waveJob?.cancel()
        waveJob = null
    }

    /**
     * 按住开始录音时渐显浮层。
     */
    private fun fadeIn() {
        fadeAnimator?.cancel()
        if (!isVisible) {
            alpha = 0f
            visible()
        }
        fadeAnimator = animate()
            .alpha(1f)
            .setDuration(FADE_DURATION_MS)
            .withEndAction { fadeAnimator = null }
        fadeAnimator?.start()
    }

    /**
     * 松开结束录音时渐隐浮层，动画结束后再隐藏。
     */
    private fun fadeOut() {
        fadeAnimator?.cancel()
        fadeAnimator = animate()
            .alpha(0f)
            .setDuration(FADE_DURATION_MS)
            .withEndAction {
                gone()
                fadeAnimator = null
            }
        fadeAnimator?.start()
    }

    /**
     * 根据相位和实时音量刷新每根波形条的高度，形成持续跳动效果。
     */
    private fun renderWave(phase: Float) {
        val center = (waveLayout.childCount - 1) / 2f
        for (index in 0 until waveLayout.childCount) {
            val distance = kotlin.math.abs(index - center) / center
            val pulse = ((kotlin.math.sin(index * 0.85f + phase) + 1f) / 2f).coerceIn(0f, 1f)
            val baseHeight = WAVE_BASE_HEIGHTS.getOrElse(index) { 12 }.dp
            val volumeScale = 0.42f + waveVolume * 1.05f
            val pulseScale = 0.65f + pulse * 0.45f
            val height = (baseHeight * pulseScale * volumeScale).toInt().coerceAtLeast(3.dp)
            val alpha = (0.22f + waveVolume * 0.38f + (1f - distance).coerceIn(0f, 1f) * 0.4f).coerceIn(0.22f, 1f)
            waveLayout.getChildAt(index).apply {
                this.alpha = alpha
            }.updateLayoutParams<LinearLayout.LayoutParams> {
                this.height = height
            }
        }
    }

    /**
     * View 脱离窗口时释放动画任务，避免协程持有旧页面引用。
     */
    override fun onDetachedFromWindow() {
        fadeAnimator?.cancel()
        fadeAnimator = null
        stopWaveAnimation()
        super.onDetachedFromWindow()
    }
}
