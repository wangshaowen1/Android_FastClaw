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
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundReportGeneratingBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 公司背调报告 - 生成中
 */
class AgentCompanyBackgroundReportGeneratingViewHolder(private val binding: ItemMsgAgentCompanyBackgroundReportGeneratingBinding) : RecyclerView.ViewHolder(binding.root) {

    /** 主线程定时刷新步骤状态，报告生成阶段需要每秒推进一次。 */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 服务端 createTime 解析器，用于计算报告生成已耗时长。 */
    private val createTimeFormat = SimpleDateFormat(CREATE_TIME_FORMAT, Locale.CHINA).apply {
        isLenient = false
    }

    /** 顶部“生成中”主图标旋转动画。 */
    private var rotateGenerateAnimator: ObjectAnimator? = null

    /** 报告创建时间戳，解析失败时为空并按当前时间兜底。 */
    private var createTimeMs: Long? = null

    /** 各步骤 loading 图标对应的旋转动画，按 ImageView 管理便于单独停止。 */
    private val rotateStepAnimators = mutableMapOf<ImageView, ObjectAnimator>()

    /** 当前 item 是否处于可见旋转状态，离屏后会置为 false 并停止刷新。 */
    private var rotating = false

    /** 绑定公司背调报告生成中卡片，并根据 createTime 刷新步骤完成状态。 */
    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CompanyBackgroundReportPayloadBean::class.java) }.getOrNull()
        }
        createTimeMs = parseCreateTimeMs(payload?.createTime)
        refreshStepState()
    }

    /** item 进入窗口时启动主图标和未完成步骤的旋转动画。 */
    fun startRotate() {
        rotating = true
        refreshStepState()
        mainHandler.removeCallbacks(refreshStepRunnable)
        mainHandler.postDelayed(refreshStepRunnable, STEP_REFRESH_INTERVAL_MS)
        if (null == rotateGenerateAnimator || rotateGenerateAnimator?.isStarted == false) {
            // 主 loading 图标只保留一个动画实例，避免重复 start 叠加。
            rotateGenerateAnimator = ObjectAnimator.ofFloat(binding.ivGenerating, "rotation", 0f, 360f).apply {
                duration = 1000L
                interpolator = LinearInterpolator()
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                start()
            }
        }
    }

    /** item 离屏或回收时停止所有旋转动画和定时刷新。 */
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

    /** 根据生成开始时间刷新步骤完成态，并让所有未完成步骤保持 loading 动画。 */
    private fun refreshStepState() {
        val elapsedSeconds = ((System.currentTimeMillis() - (createTimeMs ?: System.currentTimeMillis())) / 1000L).coerceAtLeast(0L)
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
            // 步骤完成后要移除对应 loading 动画，只保留未完成步骤继续旋转。
            syncStepRotates(unfinishedIcons)
        }
    }

    /** 同步步骤文案颜色：已完成高亮，未完成置灰。 */
    private fun refreshStepTextColor(step1Done: Boolean, step2Done: Boolean) {
        val context = binding.root.context
        val doneColor = ContextCompat.getColor(context, R.color.color_96_000000)
        val unfinishedColor = ContextCompat.getColor(context, R.color.color_55_3c3c43)
        binding.tvStep1.setTextColor(if (step1Done) doneColor else unfinishedColor)
        binding.tvStep2.setTextColor(if (step2Done) doneColor else unfinishedColor)
        binding.tvStep3.setTextColor(unfinishedColor)
    }

    /** 对齐当前未完成步骤图标的旋转动画，移除已完成步骤动画。 */
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

    /** 停止单个步骤图标动画并重置角度，避免复用时残留旋转状态。 */
    private fun stopStepRotate(icon: ImageView) {
        rotateStepAnimators.remove(icon)?.apply {
            cancel()
            removeAllListeners()
            removeAllUpdateListeners()
        }
        icon.rotation = 0f
    }

    /** 停止所有步骤图标动画，用于 item 滑出或回收。 */
    private fun stopStepRotates() {
        rotateStepAnimators.keys.toList().forEach { stopStepRotate(it) }
    }

    /** 将服务端创建时间转成时间戳，解析失败时回退为从当前时刻开始计时。 */
    private fun parseCreateTimeMs(createTime: String?): Long? {
        return createTime
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { createTimeFormat.parse(it)?.time }.getOrNull() }
    }

    /** 生成中状态的定时刷新任务，仅在 item 可见并旋转时继续自循环。 */
    private val refreshStepRunnable = object : Runnable {
        override fun run() {
            refreshStepState()
            if (rotating) {
                mainHandler.postDelayed(this, STEP_REFRESH_INTERVAL_MS)
            }
        }
    }

    companion object {
        /** 服务端报告创建时间格式。 */
        private const val CREATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

        /** 步骤状态刷新间隔，单位毫秒。 */
        private const val STEP_REFRESH_INTERVAL_MS = 1000L

        /** 第一步展示完成态所需的生成时长，单位秒。 */
        private const val STEP1_DONE_SECONDS = 30L

        /** 第二步展示完成态所需的生成时长，单位秒。 */
        private const val STEP2_DONE_SECONDS = 60L
    }
}
