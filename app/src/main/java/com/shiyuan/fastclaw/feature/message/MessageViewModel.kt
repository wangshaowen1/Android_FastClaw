package com.shiyuan.fastclaw.feature.message

import androidx.lifecycle.viewModelScope
import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_CONVERSATION
import com.shiyuan.fastclaw.feature.message.data.MessageListItem
import com.shiyuan.fastclaw.feature.message.data.UnsuitableEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// 消息会话列表 ViewModel，负责普通会话列表和“不合适会话”入口。
class MessageViewModel : BaseConversationViewModel() {

    /**
     * 拉取普通 IM 会话列表，排除已标记不合适的会话。
     */
    override fun getConversationList() {
        if (!isPageActive) return
        loadConversationsJob?.cancel()
        loadConversationsJob = viewModelScope.launch {
            isLoading.set(true)
            try {
                val allConversations = queryP2pConversations()
                val normalConversations = allConversations.filter { conversation ->
                    !isMarkedUnsuitable(conversation)
                }
                val items = mutableListOf<MessageListItem>()
                normalConversations.forEach { conversation ->
                    items.add(buildConversationItem(conversation, TYPE_CONVERSATION))
                }
                if (allConversations.size - normalConversations.size > 0) {
                    items.add(UnsuitableEntry(allConversations.size - normalConversations.size))
                }
                refreshMessageList(items)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                refreshMessageList(emptyList())
            } finally {
                isLoading.set(false)
            }
        }
    }
}
