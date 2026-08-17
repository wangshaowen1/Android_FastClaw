package com.shiyuan.fastclaw.feature.message

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.tulele.im.ImSession
import com.tulele.im.conversation.ConversationListener
import com.tulele.im.conversation.IConversationManager
import com.tulele.im.entity.Conversation
import com.tulele.im.entity.ConversationType
import com.tulele.im.entity.MessageType
import com.tulele.im.utils.AccountHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MessageViewModel : AppRepositoryViewModel<AppRepository>(), ConversationListener {

    val messageList = MutableLiveData<List<MessageListItem>>(emptyList())
    val isEmpty = ObservableBoolean(true)
    val isLoading = ObservableBoolean(false)

    private val conversationManager get() = ImSession.get().getConversationManager()
    private val userManager get() = ImSession.get().getUserManager()
    private var loadConversationsJob: Job? = null
    private var pageState = false
    private var registerConversationManager: IConversationManager? = null

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
                val conversations = queryImConversations().orEmpty()
                    .filter { conversation ->
                        conversation.conversationType == ConversationType.P2P &&
                            !conversation.contactId.isNullOrBlank()
                    }
                    .sortedByDescending { it.time }
                val users = fetchUsers(conversations.mapNotNull { it.contactId }.distinct())
                    .associateBy { it.account }
                val items = conversations.map { conversation ->
                    val account = conversation.contactId.orEmpty()
                    val user = users[account]
                    MessageConversationItem(
                        id = account,
                        targetName = user?.name?.takeIf { it.isNotBlank() } ?: account,
                        targetDesc = user?.signature?.takeIf { it.isNotBlank() },
                        targetAvatar = user?.avatar,
                        lastMessage = formatLastMessage(conversation),
                        lastMessageTime = conversation.time,
                        unreadCount = conversation.unreadCount,
                    )
                }
                updateMessageList(items)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                updateMessageList(emptyList())
            } finally {
                isLoading.set(false)
            }
        }
    }

    /**
     * 会话点击入口，本轮只做列表，后续接入单聊详情页时在这里统一跳转。
     */
    fun openConversation(item: MessageConversationItem) {
        // 这轮只做消息 Tab 和会话列表，单聊详情页后续接入。
    }

    /**
     * 已标记不合适会话入口，本轮保留点击出口，具体页面后续接入。
     */
    fun openMarkedUnsuitable() {
        // 这轮只实现入口类型与展示，具体页面后续接入。
    }

    /**
     * 查询 IM SDK 的本地会话，未登录或 SDK 查询失败都按空列表处理，避免消息 Tab 崩溃。
     */
    private suspend fun queryImConversations(): List<Conversation>? {
        if (!AccountHelper.isLogged()) return emptyList()
        return try {
            conversationManager.queryConversations(listOf(MessageType.notification))
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
    private fun updateMessageList(conversations: List<MessageConversationItem>) {
        val markedUnsuitable = queryMarkedUnsuitableEntry()
        val items = buildList {
            addAll(conversations)
            markedUnsuitable?.let { add(it) }
        }
        messageList.value = items
        isEmpty.set(items.isEmpty())
    }

    /**
     * 查询“已标记不合适的会话”入口数据，当前分支暂无数据源，先不展示假入口。
     */
    private fun queryMarkedUnsuitableEntry(): UnsuitableConversationItem? = null

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
            MessageType.avchat -> App.instance.getString(R.string.message_preview_video_call)
            MessageType.custom -> App.instance.getString(R.string.message_preview_custom)
            else -> ""
        }
    }

    /**
     * 绑定 IM 会话列表监听，manager 未变化时跳过，避免页面恢复时重复注册。
     */
    private fun registerConversationListener() {
        val manager = conversationManager
        if (registerConversationManager === manager) return
        registerConversationManager?.removeConversationListener(this)
        manager.addConversationListener(this)
        registerConversationManager = manager
    }

    /**
     * 移除已绑定的 IM 会话监听，避免 ViewModel 销毁后继续持有回调。
     */
    private fun removeConversationListener() {
        registerConversationManager?.removeConversationListener(this)
        registerConversationManager = null
    }

    /**
     * IM 首次加载会话列表后刷新消息 Tab，确保登录后会话回填到页面。
     */
    override fun onConversationsLoaded(conversations: List<Conversation>?) {
        refreshConversations()
    }

    /**
     * 会话列表新增或更新时刷新消息 Tab，覆盖新消息、撤回和最近会话排序变化。
     */
    override fun onConversationsChanged(conversations: List<Conversation>) {
        refreshConversations()
    }

    /**
     * 会话被删除时刷新消息 Tab，避免已删除会话继续停留在列表中。
     */
    override fun onConversationDeleted(conversation: Conversation) {
        refreshConversations()
    }

    /**
     * 会话状态变化时刷新消息 Tab，覆盖未读数、发送状态等局部状态变化。
     */
    override fun onConversationStateChanged(conversation: Conversation) {
        refreshConversations()
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
