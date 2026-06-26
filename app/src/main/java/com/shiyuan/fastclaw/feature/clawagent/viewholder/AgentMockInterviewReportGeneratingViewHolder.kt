package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewReportGeneratingBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 面试复盘报告 - 生成中
 */
class AgentMockInterviewReportGeneratingViewHolder(private val binding: ItemMsgAgentMockInterviewReportGeneratingBinding) : RecyclerView.ViewHolder(binding.root) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val interviewTimeFormat = SimpleDateFormat(INTERVIEW_TIME_FORMAT, Locale.CHINA).apply {
        isLenient = false
    }
    private var rotateStepAnimator: ObjectAnimator? = null
    private var rotateGenerateAnimator: ObjectAnimator? = null
    private var interviewTimeMs: Long? = null
    private var rotatingStepIcon: ImageView? = null
    private var rotating = false

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewReportPayloadBean::class.java) }.getOrNull()
        }
        interviewTimeMs = parseInterviewTimeMs(payload?.interviewTime)
        refreshStepState()
    }

    fun startRotate() {
        rotating = true
        refreshStepState()
        mainHandler.removeCallbacks(refreshStepRunnable)
        mainHandler.postDelayed(refreshStepRunnable, STEP_REFRESH_INTERVAL_MS)
        if (null == rotateGenerateAnimator || rotateGenerateAnimator?.isStarted == false) {
            rotateGenerateAnimator = ObjectAnimator.ofFloat(binding.ivGenerating, "rotation", 0f, 360f).apply {
                duration = 1000L
                interpolator = LinearInterpolator()
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                start()
            }
        }
    }

    fun stopRotate() {
        rotating = false
        mainHandler.removeCallbacks(refreshStepRunnable)
        stopStepRotate()
        rotateGenerateAnimator?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        rotateGenerateAnimator = null
        binding.ivGenerating.rotation = 0f
    }

    private fun refreshStepState() {
        val elapsedSeconds = ((System.currentTimeMillis() - (interviewTimeMs ?: System.currentTimeMillis())) / 1000L).coerceAtLeast(0L)
        binding.tvStep1Icon.setImageResource(
            if (elapsedSeconds >= STEP1_DONE_SECONDS) R.drawable.ic_company_background_report_check
            else R.drawable.ic_company_background_report_loading
        )
        binding.tvStep2Icon.setImageResource(
            if (elapsedSeconds >= STEP2_DONE_SECONDS) R.drawable.ic_company_background_report_check_2
            else R.drawable.ic_company_background_report_loading
        )
        binding.tvStep3Icon.setImageResource(R.drawable.ic_company_background_report_loading)
        val currentStepIcon = when {
            elapsedSeconds >= STEP2_DONE_SECONDS -> binding.tvStep3Icon
            elapsedSeconds >= STEP1_DONE_SECONDS -> binding.tvStep2Icon
            else -> binding.tvStep1Icon
        }
        if (rotating) {
            startStepRotate(currentStepIcon)
        }
    }

    private fun startStepRotate(icon: ImageView) {
        if (rotateStepAnimator?.isStarted == true && rotatingStepIcon === icon) return
        stopStepRotate()
        rotatingStepIcon = icon
        rotateStepAnimator = ObjectAnimator.ofFloat(icon, "rotation", 0f, 360f).apply {
            duration = 1000L
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    private fun stopStepRotate() {
        rotateStepAnimator?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        rotatingStepIcon?.rotation = 0f
        rotateStepAnimator = null
        rotatingStepIcon = null
    }

    private fun parseInterviewTimeMs(interviewTime: String?): Long? {
        return interviewTime
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { interviewTimeFormat.parse(it)?.time }.getOrNull() }
    }

    private val refreshStepRunnable = object : Runnable {
        override fun run() {
            refreshStepState()
            if (rotating) {
                mainHandler.postDelayed(this, STEP_REFRESH_INTERVAL_MS)
            }
        }
    }

    companion object {
        private const val INTERVIEW_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
        private const val STEP_REFRESH_INTERVAL_MS = 1000L
        private const val STEP1_DONE_SECONDS = 30L
        private const val STEP2_DONE_SECONDS = 60L
    }
}
