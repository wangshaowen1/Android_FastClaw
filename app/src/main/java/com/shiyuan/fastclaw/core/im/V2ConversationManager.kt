package com.shiyuan.fastclaw.core.im

import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.v2.V2NIMError
import com.netease.nimlib.sdk.v2.conversation.V2NIMConversationListener
import com.netease.nimlib.sdk.v2.conversation.V2NIMConversationService
import com.netease.nimlib.sdk.v2.conversation.model.V2NIMConversation
import com.netease.nimlib.sdk.v2.conversation.params.V2NIMConversationFilter
import com.netease.nimlib.sdk.v2.conversation.result.V2NIMConversationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// 云信 V2 会话管理器，隔离页面层与 SDK 会话接口。
object V2ConversationManager {

    // 单页会话拉取数量，云信建议不要超过 100。
    private const val PAGE_LIMIT = 100

    // 页面层监听与云信监听的映射，避免同一个监听对象重复注册。
    private val listenerMap = mutableMapOf<ImConversationChangeListener, V2NIMConversationListener>()

    // 云信会话服务。
    private val conversationService: V2NIMConversationService?
        get() = NIMClient.getService(V2NIMConversationService::class.java)

    /**
     * 分页拉取 IM 会话列表，SDK 未就绪时返回空列表。
     */
    suspend fun getConversationList(): List<V2NIMConversation> {
        val service = conversationService ?: return emptyList()
        val conversations = mutableListOf<V2NIMConversation>()
        var offset = 0L
        do {
            val result = getConversationPage(service, offset) ?: return conversations
            conversations.addAll(result.conversationList.orEmpty())
            offset = result.offset
        } while (!result.isFinished)
        return conversations
    }

    /**
     * 获取 IM 会话总未读数，SDK 查询异常时兜底为 0。
     */
    fun getTotalUnreadCount(): Int {
        val service = conversationService ?: return 0
        return try {
            service.getTotalUnreadCount()
        } catch (_: Exception) {
            0
        }
    }

    /**
     * 注册 IM 会话监听，同一个监听对象只注册一次。
     */
    fun addConversationListener(listener: ImConversationChangeListener): Boolean {
        val service = conversationService ?: return false
        if (listenerMap.containsKey(listener)) return true
        val sdkListener = listener.toSdkListener()
        return try {
            service.addConversationListener(sdkListener)
            listenerMap[listener] = sdkListener
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 移除已注册的 IM 会话监听。
     */
    fun removeConversationListener(listener: ImConversationChangeListener) {
        val service = conversationService
        val sdkListener = listenerMap.remove(listener) ?: return
        try {
            service?.removeConversationListener(sdkListener)
        } catch (_: Exception) {
        }
    }

    /**
     * 拉取单页 IM 会话列表，失败时返回 null 交由上层降级。
     */
    private suspend fun getConversationPage(service: V2NIMConversationService, offset: Long, ): V2NIMConversationResult? =
        suspendCancellableCoroutine { continuation ->
            service.getConversationList(
                offset,
                PAGE_LIMIT,
                { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                },
                {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            )
        }

    /**
     * 将业务监听转换为云信监听，统一收敛列表变化和未读变化事件。
     */
    private fun ImConversationChangeListener.toSdkListener(): V2NIMConversationListener =
        object : V2NIMConversationListener {
            /**
             * 会话同步开始，当前列表页不需要处理加载态。
             */
            override fun onSyncStarted() = Unit

            /**
             * 会话同步完成后刷新会话列表。
             */
            override fun onSyncFinished() {
                this@toSdkListener.onConversationChanged()
            }

            /**
             * 会话同步失败时刷新一次列表，让页面走本地兜底状态。
             */
            override fun onSyncFailed(error: V2NIMError?) {
                this@toSdkListener.onConversationChanged()
            }

            /**
             * 新建会话后刷新会话列表。
             */
            override fun onConversationCreated(conversation: V2NIMConversation?) {
                this@toSdkListener.onConversationChanged()
            }

            /**
             * 删除会话后刷新会话列表。
             */
            override fun onConversationDeleted(conversationIds: MutableList<String>?) {
                this@toSdkListener.onConversationChanged()
            }

            /**
             * 会话内容或排序变化后刷新会话列表。
             */
            override fun onConversationChanged(conversationList: MutableList<V2NIMConversation>?) {
                this@toSdkListener.onConversationChanged()
            }

            /**
             * 总未读数变化时转发给页面层。
             */
            override fun onTotalUnreadCountChanged(unreadCount: Int) {
                this@toSdkListener.onTotalUnreadCountChanged(unreadCount)
            }

            /**
             * 过滤条件未读数变化时刷新会话列表，避免局部订阅遗漏。
             */
            override fun onUnreadCountChangedByFilter(filter: V2NIMConversationFilter?, unreadCount: Int) {
                this@toSdkListener.onConversationChanged()
            }

            /**
             * 多端已读时间戳变化时刷新会话列表。
             */
            override fun onConversationReadTimeUpdated(conversationId: String?, readTime: Long) {
                this@toSdkListener.onConversationChanged()
            }
        }
}

// IM 会话变化监听，供页面层订阅列表与未读数变化。
interface ImConversationChangeListener {
    /**
     * 会话列表发生变化时回调。
     */
    fun onConversationChanged()

    /**
     * 总未读数发生变化时回调。
     */
    fun onTotalUnreadCountChanged(unreadCount: Int) = Unit
}
