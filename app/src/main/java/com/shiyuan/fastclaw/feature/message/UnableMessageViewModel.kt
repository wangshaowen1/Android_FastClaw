package com.shiyuan.fastclaw.feature.message

import androidx.lifecycle.viewModelScope
import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_UNSUITABLE
import com.shiyuan.fastclaw.feature.message.data.MessageListItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// 已标记不合适会话 ViewModel，只负责不合适会话列表。
class UnableMessageViewModel : BaseConversationViewModel() {

    /**
     * 拉取已标记不合适的 IM 会话列表。
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
