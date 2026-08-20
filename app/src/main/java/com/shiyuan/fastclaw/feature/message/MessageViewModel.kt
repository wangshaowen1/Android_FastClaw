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
        loadConversationsJob?.cancel()
        loadConversationsJob = viewModelScope.launch {
            isLoading.set(true)
            try {
                refreshConversationCache(queryP2pConversations())
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                refreshMessageList(emptyList())
            } finally {
                isLoading.set(false)
            }
        }
    }

    /**
     * 根据缓存生成普通会话列表，并在存在不合适会话时追加入口。
     */
    override fun refreshConversationList() {
        val items = mutableListOf<MessageListItem>()
        var unsuitableConversationCount = 0
        getCachedConversations().forEach { conversation ->
            if (!isMarkedUnsuitable(conversation)) {
                items.add(buildConversationItem(conversation, TYPE_CONVERSATION))
            } else {
                unsuitableConversationCount++
            }
        }
        if (unsuitableConversationCount > 0) {
            items.add(UnsuitableEntry(unsuitableConversationCount))
        }
        refreshMessageList(items)
    }
}
