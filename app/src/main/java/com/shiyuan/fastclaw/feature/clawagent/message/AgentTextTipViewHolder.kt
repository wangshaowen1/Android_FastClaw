package com.shiyuan.fastclaw.feature.clawagent.message

import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgAgentTextTipBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentThinkBinding

/**
 * @desc：文本居中卡
 * @author：CaoWei
 * @date：2026/6/25 13:22
 */
class AgentTextTipViewHolder(private val binding: ItemMsgAgentTextTipBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        binding.tvContent.text = "${item.content}"
    }
}