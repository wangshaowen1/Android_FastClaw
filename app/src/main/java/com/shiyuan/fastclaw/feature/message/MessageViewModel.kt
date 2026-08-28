package com.shiyuan.fastclaw.feature.message

import android.util.Log
import com.alibaba.fastjson.JSON
import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_CONVERSATION
import com.shiyuan.fastclaw.feature.message.data.MessageListItem
import com.shiyuan.fastclaw.feature.message.data.UnsuitableEntry

// 消息会话列表 ViewModel，负责普通会话列表和“不合适会话”入口。
class MessageViewModel : BaseConversationViewModel() {

    /**
     * 根据缓存生成普通会话列表，并在存在不合适会话时追加入口。
     */
    override fun refreshConversationList() {
        val items = mutableListOf<MessageListItem>()
        var unsuitableConversationCount = 0
       // Log.d("refreshConversationList 原始数据", JSON.toJSONString(getCachedConversations()[0]))
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
