package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.core.content.ContextCompat
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
    private var rotateGenerateAnimator: ObjectAnimator? = null
    private var interviewTimeMs: Long? = null
    private val rotateStepAnimators = mutableMapOf<ImageView, ObjectAnimator>()
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
        stopStepRotates()
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
        val step1Done = elapsedSeconds >= STEP1_DONE_SECONDS
        val step2Done = elapsedSeconds >= STEP2_DONE_SECONDS
        binding.tvStep1Icon.setImageResource(if (step1Done) R.drawable.ic_company_background_report_check else R.drawable.ic_company_background_report_loading)
        binding.tvStep2Icon.setImageResource(if (step2Done) R.drawable.ic_company_background_report_check_2 else R.drawable.ic_company_background_report_loading)
        binding.tvStep3Icon.setImageResource(R.drawable.ic_company_background_report_loading)
        refreshStepTextColor(step1Done, step2Done)
        val unfinishedIcons = buildList {
            if (!step1Done) add(binding.tvStep1Icon)
            if (!step2Done) add(binding.tvStep2Icon)
            add(binding.tvStep3Icon)
        }
        if (rotating) {
            syncStepRotates(unfinishedIcons)
        }
    }

    private fun refreshStepTextColor(step1Done: Boolean, step2Done: Boolean) {
        val context = binding.root.context
        val doneColor = ContextCompat.getColor(context, R.color.color_96_000000)
        val unfinishedColor = ContextCompat.getColor(context, R.color.color_55_3c3c43)
        binding.tvStep1.setTextColor(if (step1Done) doneColor else unfinishedColor)
        binding.tvStep2.setTextColor(if (step2Done) doneColor else unfinishedColor)
        binding.tvStep3.setTextColor(unfinishedColor)
    }

    private fun syncStepRotates(icons: List<ImageView>) {
        val unfinishedIconSet = icons.toSet()
        rotateStepAnimators.keys
            .filter { it !in unfinishedIconSet }
            .toList()
            .forEach { stopStepRotate(it) }
        icons.forEach { icon ->
            if (rotateStepAnimators[icon]?.isStarted == true) return@forEach
            rotateStepAnimators[icon] = ObjectAnimator.ofFloat(icon, "rotation", 0f, 360f).apply {
                duration = 1000L
                interpolator = LinearInterpolator()
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                start()
            }
        }
    }

    private fun stopStepRotate(icon: ImageView) {
        rotateStepAnimators.remove(icon)?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        icon.rotation = 0f
    }

    private fun stopStepRotates() {
        rotateStepAnimators.keys.toList().forEach { stopStepRotate(it) }
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
