package com.shiyuan.fastclaw.feature.clawagent.message

import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgAgentLeftBinding

/** agent普通消息（靠左气泡）
 * @createtime 2026年05月25日 14:49
 * @author TooWhiteT
 */

class AgentViewHolder(private val binding: ItemMsgAgentLeftBinding) : RecyclerView.ViewHolder(binding.root) {

    private val typewriter = BaseAgentTypewriterController(binding.tvMsgContent)

    fun bind(item: ClawAgentMessageInfo, displayedLength: Int = item.content.orEmpty().length, onDisplayedLengthChange: (Int) -> Unit = {}, ) {
        typewriter.bind(item.content.orEmpty(), displayedLength, onDisplayedLengthChange)
    }

    fun startTypewriter() = typewriter.start()

    fun stopTypewriter() = typewriter.stop()
}
