package com.shiyuan.fastclaw.feature.message.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.databinding.ItemChatContentResumeDeliveryBinding
import com.shiyuan.fastclaw.feature.message.data.ChatMessageContentType
import com.shiyuan.fastclaw.feature.message.data.ResumeDeliveryChatItem

/** 简历投递内容 Provider；公共方向、时间和头像由气泡壳层处理。 */
class ResumeDeliveryChatContentProvider(
    private val itemEvents: ItemEventsViewModel<MessageChatItemEvent>,
) : ChatContentProvider<ResumeDeliveryChatItem> {
    override val contentType: ChatMessageContentType = ChatMessageContentType.RESUME_DELIVERY

    override fun create(
        inflater: LayoutInflater,
        parent: ViewGroup,
    ): ChatContentView<ResumeDeliveryChatItem> = ResumeDeliveryChatContentView(
        binding = ItemChatContentResumeDeliveryBinding.inflate(inflater, parent, false),
        itemEvents = itemEvents,
    )

    private class ResumeDeliveryChatContentView(
        private val binding: ItemChatContentResumeDeliveryBinding,
        private val itemEvents: ItemEventsViewModel<MessageChatItemEvent>,
    ) : ChatContentView<ResumeDeliveryChatItem> {
        override val root: View = binding.root

        override fun bind(item: ResumeDeliveryChatItem) {
            binding.item = item
            binding.layoutFile.setOnSingleClickListener {
                itemEvents.postEvent(MessageChatItemEvent.ResumeFile(item))
            }
            binding.executePendingBindings()
        }
    }
}
