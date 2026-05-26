package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgAgentLeftBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent普通消息（靠左气泡）
 * @createtime 2026年05月25日 14:49
 * @author TooWhiteT
 */

class AgentViewHolder(private val binding: ItemMsgAgentLeftBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        binding.tvMsgContent.text = item.content
    }
}