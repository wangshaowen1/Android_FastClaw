package com.shiyuan.fastclaw.feature.message.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.databinding.ItemChatContentResumeRequestBinding
import com.shiyuan.fastclaw.feature.message.data.ChatMessageContentType
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestChatItem

/** 简历请求内容 Provider；公共方向、时间和头像由气泡壳层处理。 */
class ResumeRequestChatContentProvider(
    private val itemEvents: ItemEventsViewModel<MessageChatItemEvent>,
) : ChatContentProvider<ResumeRequestChatItem> {
    override val contentType: ChatMessageContentType = ChatMessageContentType.RESUME_REQUEST

    override fun create(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ChatContentView<ResumeRequestChatItem> = ResumeRequestChatContentView(
        binding = ItemChatContentResumeRequestBinding.inflate(inflater, parent, false),
        itemEvents = itemEvents,
    )

    private class ResumeRequestChatContentView(
        private val binding: ItemChatContentResumeRequestBinding,
        private val itemEvents: ItemEventsViewModel<MessageChatItemEvent>,
    ) : ChatContentView<ResumeRequestChatItem> {
        override val root: View = binding.root

        override fun bind(item: ResumeRequestChatItem) {
            binding.item = item
            binding.tvAccept.setOnSingleClickListener {
                itemEvents.postEvent(MessageChatItemEvent.ResumeRequestAccept(item))
            }
            binding.tvReject.setOnSingleClickListener {
                itemEvents.postEvent(MessageChatItemEvent.ResumeRequestReject(item))
            }
            binding.executePendingBindings()
        }
    }
}
