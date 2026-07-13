package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import com.kuaige.imageloader.GlideApp
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemMsgAgentThinkBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent思考消息
 * @createtime 2026年05月25日 14:51
 * @author TooWhiteT
 */

class AgentThinkViewHolder(private val binding: ItemMsgAgentThinkBinding) : RecyclerView.ViewHolder(binding.root) {

    private val typeHandler = Handler(Looper.getMainLooper())
    private var targetText = ""
    private var typing = false
    private var pendingTypewriter = false
    private var onDisplayedLengthChange: ((Int) -> Unit)? = null
    private val typeRunnable = object : Runnable {
        override fun run() {
            val showing = binding.tvThink.text?.toString().orEmpty()
            if (showing.length < targetText.length) {
                val nextLength = showing.length + 1
                binding.tvThink.text = targetText.take(nextLength)
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
        binding.tvThink.text = targetText.take(startLength)
        this.onDisplayedLengthChange?.invoke(startLength)
        pendingTypewriter = startLength < targetText.length
        if (pendingTypewriter && binding.root.isAttachedToWindow) {
            startTypewriter()
        }
    }

    /** 播放思考中的 gif；附着到窗口时由 Adapter 调用 */
    fun startRotate() {
        GlideApp.with(binding.ivThink)
            .asGif()
            .load(R.drawable.icon_agent_think)
            .into(binding.ivThink)
        startTypewriter()
    }

    /** 停止 gif；脱离窗口或回收时由 Adapter 调用 */
    fun stopRotate() {
        stopTypewriter()
        GlideApp.with(binding.ivThink).clear(binding.ivThink)
    }

    private fun startTypewriter() {
        if (typing || !pendingTypewriter) return
        typing = true
        typeHandler.post(typeRunnable)
    }

    private fun stopTypewriter() {
        typeHandler.removeCallbacks(typeRunnable)
        if (pendingTypewriter) {
            binding.tvThink.text = targetText
            onDisplayedLengthChange?.invoke(targetText.length)
        }
        typing = false
        pendingTypewriter = false
    }

    companion object {
        private const val TYPE_INTERVAL_MS = 60L
    }

}
