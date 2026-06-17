package com.shiyuan.fastclaw.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
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

class VoiceRecordingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val WAVE_BAR_COUNT = 25

        val VOICE_CANCEL_DISTANCE = 60.dp
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
    private var canceling = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_voice_recording, this)
        contentView = findViewById(R.id.layout_voice_recording_content)
        tipView = findViewById(R.id.tv_voice_record_tip)
        waveLayout = findViewById(R.id.layout_voice_wave)
        isClickable = true
        isFocusable = true
        setupWaveBars()
    }

    fun showRecording(canceling: Boolean = false) {
        updateCanceling(canceling)
        updateVolume(0f)
        startWaveAnimation()
        visible()
    }

    fun hideRecording() {
        stopWaveAnimation()
        updateVolume(0f)
        this.canceling = false
        gone()
    }

    fun updateCanceling(canceling: Boolean) {
        this.canceling = canceling
        contentView.setBackgroundResource(if (canceling) R.drawable.bg_agent_voice_cancel else R.drawable.bg_agent_voice_recording)
        tipView.text = if (canceling) "松开取消" else "松开发送，上滑取消"
        tipView.setTextColor(if (canceling) R.color.color_ff5a5f.getResColor() else R.color.color_ffffff.getResColor())
        tipView.alpha = if (canceling) 0.6f else 1f
        val barBg = if (canceling) R.drawable.bg_agent_voice_bar_cancel else R.drawable.bg_agent_voice_bar
        for (index in 0 until waveLayout.childCount) {
            waveLayout.getChildAt(index).setBackgroundResource(barBg)
        }
    }

    fun updateVolume(rmsDb: Float) {
        waveVolume = ((rmsDb + 2f) / 12f).coerceIn(0.15f, 1f)
    }

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

    private fun startWaveAnimation() {
        waveJob?.cancel()
        waveJob = (findViewTreeLifecycleOwner()?.lifecycleScope ?: return).launch {
            var phase = 0f
            while (isActive) {
                renderWave(phase)
                phase += 0.35f
                delay(48L)
            }
        }
    }

    private fun stopWaveAnimation() {
        waveJob?.cancel()
        waveJob = null
    }

    private fun renderWave(phase: Float) {
        val center = (waveLayout.childCount - 1) / 2f
        for (index in 0 until waveLayout.childCount) {
            val distance = kotlin.math.abs(index - center) / center
            val pulse = ((kotlin.math.sin(index * 0.85f + phase) + 1f) / 2f).coerceIn(0f, 1f)
            val baseHeight = WAVE_BASE_HEIGHTS.getOrElse(index) { 12 }.dp
            val height = (baseHeight * (0.78f + pulse * 0.32f) * (0.9f + waveVolume * 0.35f)).toInt()
            val alpha = (0.18f + (1f - distance).coerceIn(0f, 1f) * 0.82f).coerceIn(0.18f, 1f)
            waveLayout.getChildAt(index).apply {
                this.alpha = alpha
            }.updateLayoutParams<LinearLayout.LayoutParams> {
                this.height = height
            }
        }
    }

    override fun onDetachedFromWindow() {
        stopWaveAnimation()
        super.onDetachedFromWindow()
    }
}
