package com.shiyuan.fastclaw.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.view.isNotEmpty
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/**
 * 面试作答按钮上方的语音状态波形。
 *
 * 参照 VoiceRecordingView，把竖条创建、动画刷新和生命周期释放收口到自定义 View 内部。
 */
class InterviewVoiceBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private val WAVE_BASE_HEIGHTS = intArrayOf(6, 12, 18, 12, 6)
    }

    private val waveLayout = LinearLayout(context).apply {
        gravity = android.view.Gravity.CENTER
        orientation = LinearLayout.HORIZONTAL
    }
    private var waveJob: Job? = null

    init {
        addView(
            waveLayout,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            },
        )
        setupWaveBars()
    }

    /**
     * 启动作答中波形动画。
     */
    fun startWaveAnimation() {
        waveJob?.cancel()
        waveJob = (findViewTreeLifecycleOwner()?.lifecycleScope ?: return).launch {
            var phase = 0f
            while (isActive) {
                renderWave(phase, animated = true)
                phase += 0.42f
                delay(48L)
            }
        }
    }

    /**
     * 停止波形动画，并恢复成设计图里的静态状态。
     */
    fun stopWaveAnimation() {
        waveJob?.cancel()
        waveJob = null
        renderWave(phase = 0f, animated = false)
    }

    private fun setupWaveBars() {
        if (waveLayout.isNotEmpty()) return
        WAVE_BASE_HEIGHTS.forEachIndexed { index, height ->
            val bar = View(context).apply {
                setBackgroundResource(R.drawable.bg_interview_voice_badge_bar)
            }
            val params = LinearLayout.LayoutParams(3.dp, height.dp).apply {
                if (index < WAVE_BASE_HEIGHTS.lastIndex) {
                    marginEnd = 6.dp
                }
            }
            waveLayout.addView(bar, params)
        }
    }

    private fun renderWave(phase: Float, animated: Boolean) {
        val center = (waveLayout.childCount - 1) / 2f
        for (index in 0 until waveLayout.childCount) {
            val baseHeight = WAVE_BASE_HEIGHTS.getOrElse(index) { 10 }
            val height = if (animated) {
                val distance = if (center == 0f) 0f else abs(index - center) / center
                val pulse = ((sin(phase + index * 0.95f) + 1f) / 2f).coerceIn(0f, 1f)
                (baseHeight * (0.68f + pulse * 0.62f) * (1f - distance * 0.16f)).toInt().coerceAtLeast(4)
            } else {
                baseHeight
            }
            waveLayout.getChildAt(index).apply {
                alpha = if (animated) 0.45f + (height.toFloat() / 18f).coerceIn(0f, 1f) * 0.55f else 1f
            }.updateLayoutParams<LinearLayout.LayoutParams> {
                this.height = height.dp
            }
        }
    }

    override fun onDetachedFromWindow() {
        stopWaveAnimation()
        super.onDetachedFromWindow()
    }
}
