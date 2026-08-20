package com.shiyuan.fastclaw.feature.message

import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_UNSUITABLE
import com.shiyuan.fastclaw.feature.message.data.MessageListItem

// 已标记不合适会话 ViewModel，只负责不合适会话列表。
class UnableMessageViewModel : BaseConversationViewModel() {

    /**
     * 根据缓存生成已标记不合适的会话列表。
     */
    override fun refreshConversationList() {
        val items = mutableListOf<MessageListItem>()
        getCachedConversations().forEach { conversation ->
            if (isMarkedUnsuitable(conversation)) {
                items.add(buildConversationItem(conversation, TYPE_UNSUITABLE))
            }
        }
        refreshMessageList(items)
    }
}
