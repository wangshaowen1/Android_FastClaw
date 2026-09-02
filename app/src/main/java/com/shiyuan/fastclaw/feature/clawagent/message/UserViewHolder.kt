package com.shiyuan.fastclaw.feature.clawagent.message

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ClipboardUtils
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemMsgUserRightBinding

/** 用户发送消息（靠右气泡）
 * @createtime 2026年05月25日 14:47
 * @author TooWhiteT
 */

class UserViewHolder(private val binding: ItemMsgUserRightBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        binding.tvMsgContent.text = item.content
        // 长按气泡复制消息内容
        binding.tvMsgContent.setOnLongClickListener {
            val content = item.content
            if (!content.isNullOrEmpty()) {
                ClipboardUtils.copyText(content)
                R.string.copy_success.getResString().showToast()
            }
            true
        }
    }
}