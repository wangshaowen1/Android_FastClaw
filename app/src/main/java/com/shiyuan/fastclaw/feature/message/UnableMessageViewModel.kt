package com.shiyuan.fastclaw.feature.message

import androidx.lifecycle.viewModelScope
import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_UNSUITABLE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// 已标记不合适会话 ViewModel，只负责不合适会话列表。
class UnableMessageViewModel : BaseConversationViewModel() {

    /**
     * 拉取已标记不合适的 IM 会话列表。
     */
    override fun getConversationList() {
        if (!isPageActive) return
        loadConversationsJob?.cancel()
        loadConversationsJob = viewModelScope.launch {
            isLoading.set(true)
            try {
                val conversations = queryP2pConversations().filter { isMarkedUnsuitable(it) }
                val items = conversations.map { conversation ->
                    buildConversationItem(conversation, TYPE_UNSUITABLE)
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
