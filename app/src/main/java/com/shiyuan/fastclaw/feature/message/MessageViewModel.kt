package com.shiyuan.fastclaw.feature.message

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_CONVERSATION
import com.shiyuan.fastclaw.feature.message.data.ConversationItem
import com.shiyuan.fastclaw.feature.message.data.MessageListItem
import com.shiyuan.fastclaw.feature.message.data.UnsuitableEntry
import com.tulele.im.ImSession
import com.tulele.im.conversation.ConversationListener
import com.tulele.im.entity.Conversation
import com.tulele.im.entity.ConversationType
import com.tulele.im.entity.MessageType
import com.tulele.im.utils.AccountHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// 消息会话列表 ViewModel，负责加载 IM 会话并监听会话变化。
class MessageViewModel : AppRepositoryViewModel<AppRepository>() {
    // 消息页展示的会话和入口列表数据。
    val conversationList = MutableLiveData<List<MessageListItem>>(emptyList())
    // 当前列表是否为空。
    val isEmpty = ObservableBoolean(false)
    // 当前是否正在加载会话列表。
    val isLoading = ObservableBoolean(false)
    // 组件会话门面，页面不直接依赖云信 SDK 模型。
    private val conversationManager get() = ImSession.get().getConversationManager()
    // IM 用户资料管理器，用于补齐会话用户信息。
    private val userManager get() = ImSession.get().getUserManager()
    // 当前会话列表加载任务，页面离开或重新刷新时取消旧任务。
    private var loadConversationsJob: Job? = null
    // 页面是否处于可见状态，避免后台继续刷新列表。
    private var pageState = false
    // 当前 ViewModel 是否已经注册 IM 会话监听，避免重复注册监听。
    private var conversationListenerRegistered = false

    private val conversationListener = object : ConversationListener {
        override fun onConversationsLoaded(conversations: List<Conversation>?) = refreshConversations()

        override fun onConversationsChanged(conversations: List<Conversation>) = refreshConversations()

        override fun onConversationDeleted(conversation: Conversation) = refreshConversations()

        override fun onConversationStateChanged(conversation: Conversation) = refreshConversations()
    }

    /**
     * ViewModel 创建时先尝试绑定 IM 会话监听，登录完成后页面恢复会再次补注册。
     */
    override fun onCreate() {
        super.onCreate()
        registerConversationListener()
    }

    /**
     * 同步页面可见状态，页面不可见时取消本轮会话加载，避免后台继续刷新 UI。
     */
    fun setPageState(active: Boolean) {
        if (pageState == active) return
        pageState = active
        if (active) {
            registerConversationListener()
            refreshConversations()
        } else {
            cancelLoadJob()
        }
    }

    /**
     * 刷新当前消息页的 IM 会话与后端互动数据。
     */
    fun refreshConversations() {
        getConversationList()
    }

    /**
     * 拉取 IM 本地会话并映射为消息列表 UI 数据，SDK 未就绪或资料失败时降级为空态。
     */
    fun getConversationList() {
        if (!pageState) return
        loadConversationsJob?.cancel()
        loadConversationsJob = viewModelScope.launch {
            isLoading.set(true)
            try {
                val conversations = queryImConversations()
                    .filter { it.conversationType == ConversationType.P2P }
                    .filter { it.contactId.isNotBlank() }
                    .sortedByDescending { it.time }
                val (unsuitableConversations, normalConversations) = conversations.partition { conversation ->
                    isMarkedUnsuitable(conversation.contactId)
                }
                val users = fetchUsers(normalConversations.map { it.contactId }.distinct())
                    .associateBy { it.account }
                val items = normalConversations.map { conversation ->
                    val account = conversation.contactId
                    val user = users[account]
                    ConversationItem(
                        id = conversation.conversationId,
                        contactId = account,
                        itemType = TYPE_CONVERSATION,
                        targetName = user?.name?.takeIf { it.isNotBlank() } ?: account,
                        targetDesc = user?.signature?.takeIf { it.isNotBlank() },
                        targetAvatar = user?.avatar,
                        lastMessage = formatLastMessage(conversation),
                        lastMessageTime = conversation.time,
                        unreadCount = conversation.unreadCount,
                        extension = conversation.extension,
                    )
                }
                updateMessageList(items, 1)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                updateMessageList(emptyList(), 0)
            } finally {
                isLoading.set(false)
            }
        }
    }

    /**
     * 会话点击入口，本轮只做列表，后续接入单聊详情页时在这里统一跳转。
     */
    fun openConversation(item: ConversationItem) {
        // 这轮只做消息 Tab 和会话列表，单聊详情页后续接入。
    }

    /**
     * 查询 IM SDK 的本地会话，未登录或 SDK 查询失败都按空列表处理，避免消息 Tab 崩溃。
     */
    private suspend fun queryImConversations(): List<Conversation> {
        if (!AccountHelper.isLogged()) return emptyList()
        return try {
            conversationManager.queryConversations().orEmpty()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 批量补齐会话用户资料，资料失败时保留账号兜底展示，不阻断会话列表。
     */
    private suspend fun fetchUsers(accounts: List<String>) = try {
        if (accounts.isEmpty()) emptyList() else userManager.fetchUserInfo(accounts).orEmpty()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        emptyList()
    }

    /**
     * 合并普通会话与预留入口类型，并同步空态开关。
     */
    private fun updateMessageList(conversations: List<ConversationItem>, unsuitableCount: Int) {
        val markedUnsuitable = queryMarkedUnsuitableEntry(unsuitableCount)
        val items = buildList {
            addAll(conversations)
            markedUnsuitable?.let { add(it) }
        }
        conversationList.value = items
        isEmpty.set(conversations.isEmpty())
    }

    /**
     * 根据黑名单会话数量生成“已标记不合适的会话”入口，数量为空时不展示。
     */
    private fun queryMarkedUnsuitableEntry(count: Int): UnsuitableEntry? =
        count.takeIf { it > 0 }?.let { UnsuitableEntry(it) }

    /**
     * 判断当前联系人是否已被标记为不合适，SDK 查询异常时按普通会话展示。
     */
    private fun isMarkedUnsuitable(account: String): Boolean =
        try {
            ImSession.get().isInBlackList(account)
        } catch (_: Exception) {
            false
        }

    /**
     * 将 IM 末条消息转换成会话列表摘要，非文本消息展示资源文案。
     */
    private fun formatLastMessage(conversation: Conversation): String {
        conversation.content?.takeIf { it.isNotBlank() }?.let { return it }
        return when (conversation.messageType) {
            MessageType.image -> App.instance.getString(R.string.message_preview_image)
            MessageType.video -> App.instance.getString(R.string.message_preview_video)
            MessageType.audio -> App.instance.getString(R.string.message_preview_audio)
            MessageType.location -> App.instance.getString(R.string.message_preview_location)
            MessageType.file -> App.instance.getString(R.string.message_preview_file)
            MessageType.tip -> App.instance.getString(R.string.message_preview_tip)
            MessageType.notification -> App.instance.getString(R.string.message_preview_notification)
            MessageType.avchat,
            MessageType.nrtc_netcall -> App.instance.getString(R.string.message_preview_video_call)
            MessageType.custom,
            MessageType.appCustom -> App.instance.getString(R.string.message_preview_custom)
            else -> ""
        }
    }

    /**
     * 绑定 IM 会话列表监听，manager 未变化时跳过，避免页面恢复时重复注册。
     */
    private fun registerConversationListener() {
        if (conversationListenerRegistered) return
        conversationManager.addConversationListener(conversationListener)
        conversationListenerRegistered = true
    }

    /**
     * 移除已绑定的 IM 会话监听，避免 ViewModel 销毁后继续持有回调。
     */
    private fun removeConversationListener() {
        if (!conversationListenerRegistered) return
        conversationManager.removeConversationListener(conversationListener)
        conversationListenerRegistered = false
    }

    /**
     * 取消当前会话加载任务并清空引用，避免旧任务回填列表。
     */
    private fun cancelLoadJob() {
        loadConversationsJob?.cancel()
        loadConversationsJob = null
    }

    /**
     * ViewModel 销毁时取消未完成的会话加载任务。
     */
    override fun onDestroy() {
        removeConversationListener()
        cancelLoadJob()
        super.onDestroy()
    }
}
