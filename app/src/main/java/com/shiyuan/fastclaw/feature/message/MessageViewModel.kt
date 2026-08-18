package com.shiyuan.fastclaw.feature.message

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.netease.nimlib.sdk.v2.conversation.enums.V2NIMConversationType
import com.netease.nimlib.sdk.v2.conversation.model.V2NIMConversation
import com.netease.nimlib.sdk.v2.message.enums.V2NIMMessageType
import com.netease.nimlib.sdk.v2.utils.V2NIMConversationIdUtil
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.im.ImConversationChangeListener
import com.shiyuan.fastclaw.core.im.V2ConversationManager
import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_CONVERSATION
import com.shiyuan.fastclaw.feature.message.data.ConversationItem
import com.shiyuan.fastclaw.feature.message.data.MessageListItem
import com.shiyuan.fastclaw.feature.message.data.UnsuitableEntry
import com.tulele.im.ImSession
import com.tulele.im.utils.AccountHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// 消息会话列表 ViewModel，负责加载 IM 会话并监听会话变化。
class MessageViewModel : AppRepositoryViewModel<AppRepository>(), ImConversationChangeListener {
    // 消息页展示的会话和入口列表数据。
    val conversationList = MutableLiveData<List<MessageListItem>>(emptyList())
    // 当前列表是否为空。
    val isEmpty = ObservableBoolean(true)
    // 当前是否正在加载会话列表。
    val isLoading = ObservableBoolean(false)
    // 云信 V2 会话管理器，统一承接会话列表、监听和未读数能力。
    private val conversationManager get() = V2ConversationManager
    // IM 用户资料管理器，用于补齐会话用户信息。
    private val userManager get() = ImSession.get().getUserManager()
    // 当前会话列表加载任务，页面离开或重新刷新时取消旧任务。
    private var loadConversationsJob: Job? = null
    // 页面是否处于可见状态，避免后台继续刷新列表。
    private var pageState = false
    // 当前 ViewModel 是否已经注册 IM 会话监听，避免重复注册监听。
    private var conversationListenerRegistered = false

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
                    .filter { it.type == V2NIMConversationType.V2NIM_CONVERSATION_TYPE_P2P }
                    .filter { it.targetId().isNotBlank() }
                    .sortedByDescending { it.lastMessageTime() }
                val (unsuitableConversations, normalConversations) = conversations.partition { conversation ->
                    isMarkedUnsuitable(conversation.targetId())
                }
                val users = fetchUsers(normalConversations.map { it.targetId() }.distinct())
                    .associateBy { it.account }
                val items = normalConversations.map { conversation ->
                    val account = conversation.targetId()
                    val user = users[account]
                    ConversationItem(
                        id = conversation.conversationId.orEmpty(),
                        contactId = account,
                        itemType = TYPE_CONVERSATION,
                        targetName = user?.name?.takeIf { it.isNotBlank() } ?: account,
                        targetDesc = user?.signature?.takeIf { it.isNotBlank() },
                        targetAvatar = user?.avatar,
                        lastMessage = formatLastMessage(conversation),
                        lastMessageTime = conversation.lastMessageTime(),
                        unreadCount = conversation.unreadCount,
                        localExtension = conversation.localExtension,
                        serverExtension = conversation.serverExtension,
                    )
                }
                updateMessageList(items, unsuitableConversations.size)
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
    private suspend fun queryImConversations(): List<V2NIMConversation> {
        if (!AccountHelper.isLogged()) return emptyList()
        return try {
            conversationManager.getConversationList()
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
        isEmpty.set(items.isEmpty())
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
    private fun formatLastMessage(conversation: V2NIMConversation): String {
        val lastMessage = conversation.lastMessage
        lastMessage?.text?.takeIf { it.isNotBlank() }?.let { return it }
        return when (lastMessage?.messageType) {
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_IMAGE -> App.instance.getString(R.string.message_preview_image)
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_VIDEO -> App.instance.getString(R.string.message_preview_video)
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_AUDIO -> App.instance.getString(R.string.message_preview_audio)
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_LOCATION -> App.instance.getString(R.string.message_preview_location)
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_FILE -> App.instance.getString(R.string.message_preview_file)
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_TIPS -> App.instance.getString(R.string.message_preview_tip)
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_NOTIFICATION -> App.instance.getString(R.string.message_preview_notification)
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_AVCHAT,
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_CALL -> App.instance.getString(R.string.message_preview_video_call)
            V2NIMMessageType.V2NIM_MESSAGE_TYPE_CUSTOM -> App.instance.getString(R.string.message_preview_custom)
            else -> ""
        }
    }

    /**
     * 解析云信 conversationId 中的 P2P 对端账号，解析失败时返回空字符串。
     */
    private fun V2NIMConversation.targetId(): String =
        V2NIMConversationIdUtil.conversationTargetId(conversationId.orEmpty()).orEmpty()

    /**
     * 获取会话末条消息时间，没有末条消息时使用会话更新时间。
     */
    private fun V2NIMConversation.lastMessageTime(): Long =
        lastMessage?.messageRefer?.createTime ?: updateTime

    /**
     * 绑定 IM 会话列表监听，manager 未变化时跳过，避免页面恢复时重复注册。
     */
    private fun registerConversationListener() {
        if (conversationListenerRegistered) return
        conversationListenerRegistered = conversationManager.addConversationListener(this)
    }

    /**
     * 移除已绑定的 IM 会话监听，避免 ViewModel 销毁后继续持有回调。
     */
    private fun removeConversationListener() {
        if (!conversationListenerRegistered) return
        conversationManager.removeConversationListener(this)
        conversationListenerRegistered = false
    }

    /**
     * IM 会话列表变化时刷新消息 Tab，覆盖新消息、撤回、删除和最近会话排序变化。
     */
    override fun onConversationChanged() {
        refreshConversations()
    }

    /**
     * IM 总未读数变化时刷新会话列表，保证会话行未读数同步。
     */
    override fun onTotalUnreadCountChanged(unreadCount: Int) {
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
