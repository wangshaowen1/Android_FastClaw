package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgAgentLeftBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent普通消息（靠左气泡）
 * @createtime 2026年05月25日 14:49
 * @author TooWhiteT
 */

class AgentViewHolder(private val binding: ItemMsgAgentLeftBinding) : RecyclerView.ViewHolder(binding.root) {

    private val typeHandler = Handler(Looper.getMainLooper())
    private var targetText = ""
    private var typing = false
    private var pendingTypewriter = false
    private var onDisplayedLengthChange: ((Int) -> Unit)? = null
    private val typeRunnable = object : Runnable {
        override fun run() {
            val showing = binding.tvMsgContent.text?.toString().orEmpty()
            if (showing.length < targetText.length) {
                val nextLength = showing.length + 1
                binding.tvMsgContent.text = targetText.take(nextLength)
                onDisplayedLengthChange?.invoke(nextLength)
                typeHandler.postDelayed(this, TYPE_INTERVAL_MS)
            } else {
                typing = false
                pendingTypewriter = false
                onDisplayedLengthChange?.invoke(targetText.length)
            }
        }
    }

    fun bind(
        item: ClawAgentMessageInfo,
        displayedLength: Int = item.content.orEmpty().length,
        onDisplayedLengthChange: (Int) -> Unit = {},
    ) {
        stopTypewriter()
        targetText = item.content.orEmpty()
        this.onDisplayedLengthChange = onDisplayedLengthChange
        val startLength = displayedLength.coerceIn(0, targetText.length)
        binding.tvMsgContent.text = targetText.take(startLength)
        this.onDisplayedLengthChange?.invoke(startLength)
        pendingTypewriter = startLength < targetText.length
        if (pendingTypewriter && binding.root.isAttachedToWindow) {
            startTypewriter()
        }
    }

    fun startTypewriter() {
        if (typing || !pendingTypewriter) return
        typing = true
        typeHandler.post(typeRunnable)
    }

    fun stopTypewriter() {
        typeHandler.removeCallbacks(typeRunnable)
        if (pendingTypewriter) {
            binding.tvMsgContent.text = targetText
            onDisplayedLengthChange?.invoke(targetText.length)
        }
        typing = false
        pendingTypewriter = false
    }

    companion object {
        private const val TYPE_INTERVAL_MS = 60L
    }
}
