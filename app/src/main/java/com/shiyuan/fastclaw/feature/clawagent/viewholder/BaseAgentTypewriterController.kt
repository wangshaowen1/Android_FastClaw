package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.os.Handler
import android.os.Looper
import android.widget.TextView

/** Agent 文本打字机控制器：按已展示长度继续播放，避免列表复用后重复从头输出。 */
internal class BaseAgentTypewriterController(private val textView: TextView, private val intervalMs: Long = TYPE_INTERVAL_MS) {
    /** 主线程 Handler，用于逐字刷新 TextView。 */
    private val handler = Handler(Looper.getMainLooper())

    /** 本次需要完整展示的目标文本。 */
    private var targetText = ""

    /** 当前是否正在执行打字机动画。 */
    private var typing = false

    /** 是否还有未展示完的文本，View attach 后据此继续播放。 */
    private var pending = false

    /** 已展示字符数变化回调，外层用它记录每条消息的播放进度。 */
    private var onDisplayedLengthChange: ((Int) -> Unit)? = null

    /** 每次追加一个字符，直到展示长度追上目标文本长度。 */
    private val typeRunnable = object : Runnable {
        override fun run() {
            val showing = textView.text?.toString().orEmpty()
            if (showing.length < targetText.length) {
                val nextLength = showing.length + 1
                textView.text = targetText.take(nextLength)
                onDisplayedLengthChange?.invoke(nextLength)
                // 还有剩余字符时继续排下一次刷新，形成流式输出效果。
                handler.postDelayed(this, intervalMs)
            } else {
                typing = false
                pending = false
                onDisplayedLengthChange?.invoke(targetText.length)
            }
        }
    }

    /** 绑定新文本和历史展示进度，未展示完且 View 已 attach 时自动继续播放。 */
    fun bind(text: String, displayedLength: Int, onDisplayedLengthChange: (Int) -> Unit) {
        stop()
        targetText = text
        this.onDisplayedLengthChange = onDisplayedLengthChange
        // displayedLength 来自外层缓存，限制在文本长度内，避免服务端替换文本后越界。
        val startLength = displayedLength.coerceIn(0, targetText.length)
        textView.text = targetText.take(startLength)
        this.onDisplayedLengthChange?.invoke(startLength)
        pending = startLength < targetText.length
        if (pending && textView.isAttachedToWindow) {
            start()
        }
    }

    /** 开始或继续播放打字机动画；没有待播放内容时直接忽略。 */
    fun start() {
        if (typing || !pending) return
        typing = true
        handler.post(typeRunnable)
    }

    /** 停止当前动画任务，通常在 ViewHolder detach/recycle 或重新 bind 前调用。 */
    fun stop() {
        handler.removeCallbacks(typeRunnable)
        typing = false
    }

    private companion object {
        /** 默认每个字符输出间隔，单位毫秒。 */
        const val TYPE_INTERVAL_MS = 60L
    }
}
