package com.shiyuan.fastclaw.feature.message.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blankj.utilcode.util.ClipboardUtils
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemChatContentTextBinding
import com.shiyuan.fastclaw.feature.message.data.ChatMessageContentType
import com.shiyuan.fastclaw.feature.message.data.TextChatItem

/** 文本内容 Provider；左右方向共用同一份内容实现。 */
class TextChatContentProvider : ChatContentProvider<TextChatItem> {
    override val contentType: ChatMessageContentType = ChatMessageContentType.TEXT

    override fun create(inflater: LayoutInflater, parent: ViewGroup): ChatContentView<TextChatItem> =
        TextChatContentView(ItemChatContentTextBinding.inflate(inflater, parent, false))

    private class TextChatContentView(
        private val binding: ItemChatContentTextBinding,
    ) : ChatContentView<TextChatItem> {
        override val root: View = binding.root

        override fun bind(item: TextChatItem) {
            binding.item = item
            binding.tvContent.setOnLongClickListener {
                binding.root.context.getString(R.string.toast_copied).showToast()
                ClipboardUtils.copyText(item.content)
                true
            }
            binding.executePendingBindings()
        }
    }
}
