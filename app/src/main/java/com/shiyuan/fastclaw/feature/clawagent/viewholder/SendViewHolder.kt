package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemMsgUserRightBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** 用户发送消息（靠右气泡）
 * @createtime 2026年05月25日 14:47
 * @author TooWhiteT
 */

class SendViewHolder(private val binding: ItemMsgUserRightBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        binding.tvMsgContent.text = item.content
    }
}