package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.os.Handler
import android.os.Looper
import android.widget.TextView

internal class BaseAgentTypewriterController(private val textView: TextView, private val intervalMs: Long = TYPE_INTERVAL_MS) {
    private val handler = Handler(Looper.getMainLooper())
    private var targetText = ""
    private var typing = false
    private var pending = false
    private var onDisplayedLengthChange: ((Int) -> Unit)? = null

    private val typeRunnable = object : Runnable {
        override fun run() {
            val showing = textView.text?.toString().orEmpty()
            if (showing.length < targetText.length) {
                val nextLength = showing.length + 1
                textView.text = targetText.take(nextLength)
                onDisplayedLengthChange?.invoke(nextLength)
                handler.postDelayed(this, intervalMs)
            } else {
                typing = false
                pending = false
                onDisplayedLengthChange?.invoke(targetText.length)
            }
        }
    }

    fun bind(text: String, displayedLength: Int, onDisplayedLengthChange: (Int) -> Unit) {
        stop()
        targetText = text
        this.onDisplayedLengthChange = onDisplayedLengthChange
        val startLength = displayedLength.coerceIn(0, targetText.length)
        textView.text = targetText.take(startLength)
        this.onDisplayedLengthChange?.invoke(startLength)
        pending = startLength < targetText.length
        if (pending && textView.isAttachedToWindow) {
            start()
        }
    }

    fun start() {
        if (typing || !pending) return
        typing = true
        handler.post(typeRunnable)
    }

    fun stop() {
        handler.removeCallbacks(typeRunnable)
        typing = false
    }

    private companion object {
        const val TYPE_INTERVAL_MS = 60L
    }
}
