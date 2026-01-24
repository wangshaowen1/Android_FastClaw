package com.kgmedia.fastfilm.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.doOnLayout
import com.bumptech.glide.Glide
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.network.apis.entity.DanmakuItem
import com.nhsd.basic.ext.dp
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max




/**
 * 弹幕控件
 */
class DanmakuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var danmakuList: List<DanmakuItem> = emptyList()

    // ===================== 配置参数 =====================
    var lineCount: Int = 2
    var lineSpacing: Float = 12f.dp.toFloat()
    var minDanmakuSpacing: Float = 40f.dp.toFloat()

    // 修复：限制最小速度，防止除零；
    var danmakuSpeed: Float = 0.3f
        set(value) { field = max(0.05f, value) }

    // 新增：固定行高（建议在 XML 里配置或者根据某个参考值设定）
    // 如果为 0，则动态计算（会有抖动风险）；如果 >0，则强制每行高度
    var fixedLineHeight: Int = 0

    // ===================== 内部状态 =====================
    @Volatile private var isRunning = false
    @Volatile private var isPaused = false
    private var currentIndex = 0

    private val lastViewInLines = HashMap<Int, View?>()
    private val activeAnimators = CopyOnWriteArrayList<ObjectAnimator>()

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.DanmakuView)
            lineCount = typedArray.getInt(R.styleable.DanmakuView_lineCount, lineCount)
            lineSpacing = typedArray.getDimension(R.styleable.DanmakuView_lineSpacing, lineSpacing)
            minDanmakuSpacing = typedArray.getDimension(R.styleable.DanmakuView_minDanmakuSpacing, minDanmakuSpacing)
            danmakuSpeed = typedArray.getFloat(R.styleable.DanmakuView_danmakuSpeed, danmakuSpeed)
            // 假设你在 xml 里加了这个属性，如果没有就手动设
            // fixedLineHeight = typedArray.getDimensionPixelSize(R.styleable.DanmakuView_fixedLineHeight, 0)
            typedArray.recycle()
        }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    // ===================== 控制接口 =====================

    fun start(list: List<DanmakuItem>) {
        if (list.isEmpty()) return
        stop() // 先清理

        danmakuList = list
        currentIndex = 0
        isRunning = true
        isPaused = false

        // 修复脏数据问题：Start 时彻底重置 Map
        lastViewInLines.clear()

        post(dispatchRunnable)
    }

    fun stop() {
        isRunning = false
        removeCallbacks(dispatchRunnable)

        // 【优化】快照 + 移除监听 + 强制结束
        // 这样不会触发 onAnimationEnd，避免了 removeView 的二次调用
        val animatorsToCancel = ArrayList(activeAnimators)
        activeAnimators.clear()

        animatorsToCancel.forEach { animator ->
            animator.removeAllListeners() // 关键：切断回调
            animator.cancel() // 或者 animator.end()
        }

        removeAllViews()
        lastViewInLines.clear()
    }

    fun pause() {
        if (!isRunning || isPaused) return
        isPaused = true
        removeCallbacks(dispatchRunnable)
        activeAnimators.forEach { if (it.isRunning) it.pause() }
    }

    fun resume() {
        if (!isRunning || !isPaused) return
        isPaused = false
        activeAnimators.forEach { animator ->
            if (animator.isPaused) animator.resume()
            else if (!animator.isStarted) animator.start()
        }
        post(dispatchRunnable)
    }

    // ===================== 核心引擎 =====================

    private val dispatchRunnable = object : Runnable {
        override fun run() {
            if (!isRunning || danmakuList.isEmpty()) return

            if (!isPaused) {
                tryAddDanmaku()
            }
            postDelayed(this, 200)
        }
    }

    private fun tryAddDanmaku() {
        for (i in 0 until lineCount) {
            if (canAddDanmakuToLine(i)) {
                val item = danmakuList[currentIndex]
                addDanmakuView(item, i)
                currentIndex = (currentIndex + 1) % danmakuList.size
            }
        }
    }

    private fun canAddDanmakuToLine(line: Int): Boolean {
        val lastView = lastViewInLines[line] ?: return true
        if (lastView.width == 0) return false

        val tailX = lastView.translationX + lastView.width
        return tailX < (width - minDanmakuSpacing)
    }

    private fun addDanmakuView(item: DanmakuItem, line: Int) {
        val context = context ?: return
        if (context is android.app.Activity && context.isDestroyed) return

        val view = LayoutInflater.from(context).inflate(R.layout.item_danmaku, this, false)
        val avatar = view.findViewById<ImageView>(R.id.ivAvatar)
        val text = view.findViewById<TextView>(R.id.tvText)

        // 修复：使用 applicationContext 或者 确保 context 存活
        // 这里依然用 context，但在 stop 里我们可以做清理（虽然 Glide 自动管理 RequestManager）
        Glide.with(context).load(item.avatarUrl).circleCrop().into(avatar)
        text.text = item.tipContext

        view.translationX = width.toFloat() + 500f

        addView(view)
        lastViewInLines[line] = view

        view.doOnLayout { targetView ->
            if (!isRunning) {
                removeView(targetView)
                return@doOnLayout
            }

            // 修复：行高计算策略
            val currentItemHeight = targetView.height
            // 如果设置了 fixedLineHeight，就用固定的，否则用当前的（可能会抖动）
            val lineHeightToUse = if (fixedLineHeight > 0) fixedLineHeight else currentItemHeight

            val topMargin = line * (lineHeightToUse + lineSpacing)
            targetView.translationY = topMargin.toFloat()

            // 归位到屏幕右边缘
            targetView.translationX = width.toFloat()

            val screenWidth = width
            val viewWidth = targetView.width
            val totalDistance = screenWidth + viewWidth

            // 修复：限制最大时长，防止过长动画导致的不可预知问题（虽然不会 ANR）
            val rawDuration = (totalDistance / danmakuSpeed).toLong()
            val duration = rawDuration.coerceAtMost(30_000L) // 上限 30秒

            val animator = ObjectAnimator.ofFloat(targetView, "translationX", width.toFloat(), -viewWidth.toFloat())
            animator.duration = duration
            animator.interpolator = LinearInterpolator()

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 这里的代码只有在自然播放结束时才会执行
                    // 因为 stop() 里我们移除了 listeners
                    removeView(targetView)
                    activeAnimators.remove(animator)
                    if (lastViewInLines[line] == targetView) {
                        lastViewInLines[line] = null
                    }
                }
            })

            activeAnimators.add(animator)

            if (!isPaused) {
                animator.start()
            }
        }
    }
}