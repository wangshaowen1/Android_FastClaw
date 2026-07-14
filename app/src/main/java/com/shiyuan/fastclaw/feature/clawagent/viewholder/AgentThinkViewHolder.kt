package com.shiyuan.fastclaw.feature.clawagent.viewholder

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

    private val typewriter = BaseAgentTypewriterController(binding.tvThink)

    fun bind(item: ClawAgentMessageInfo, displayedLength: Int = item.content.orEmpty().length, onDisplayedLengthChange: (Int) -> Unit = {}, ) {
        GlideApp.with(binding.ivThink)
            .asGif()
            .load(R.drawable.icon_agent_think)
            .into(binding.ivThink)
        typewriter.bind(item.content.orEmpty(), displayedLength, onDisplayedLengthChange)
    }

    fun startTypewriter() = typewriter.start()

    fun stopTypewriter() = typewriter.stop()

}
