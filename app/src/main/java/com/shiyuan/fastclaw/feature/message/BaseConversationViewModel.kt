package com.shiyuan.fastclaw.feature.message

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.feature.message.data.ConversationItem
import com.shiyuan.fastclaw.feature.message.data.MessageListItem
import com.tulele.im.ImSession
import com.tulele.im.conversation.ConversationListener
import com.tulele.im.entity.Conversation
import com.tulele.im.entity.ConversationType
import com.tulele.im.entity.MessageType
import com.tulele.im.utils.AccountHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlin.jvm.JvmName

// 消息会话列表基类，封装 IM 会话查询、监听和通用列表映射。
abstract class BaseConversationViewModel : AppRepositoryViewModel<AppRepository>() {
    // 消息页展示的会话和入口列表数据。
    @get:JvmName("getConversationListLiveData")
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
    protected var loadConversationsJob: Job? = null
    // 页面是否处于可见状态，避免后台继续刷新列表。
    protected var isPageActive = false
    // 当前 ViewModel 是否已经注册 IM 会话监听，避免重复注册监听。
    private var conversationListenerRegistered = false

    private val conversationListener = object : ConversationListener {
        /**
         * 首次加载会话完成时刷新当前页面列表。
         */
        override fun onConversationsLoaded(conversations: List<Conversation>?) = refreshConversations()

        /**
         * 会话内容发生变化时刷新当前页面列表。
         */
        override fun onConversationsChanged(conversations: List<Conversation>) = refreshConversations()

        /**
         * 会话被删除时刷新当前页面列表。
         */
        override fun onConversationDeleted(conversation: Conversation) = refreshConversations()

        /**
         * 会话状态变化时刷新当前页面列表。
         */
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
        if (isPageActive == active) return
        isPageActive = active
        if (active) {
            registerConversationListener()
            refreshConversations()
        } else {
            cancelLoadJob()
        }
    }

    /**
     * 刷新当前页面的 IM 会话列表。
     */
    protected fun refreshConversations() {
        getConversationList()
    }

    /**
     * 会话点击入口，后续接入单聊详情页时在这里统一跳转。
     */
    fun openConversation(item: ConversationItem) {
        // 单聊详情页后续接入。
    }

    /**
     * 查询 IM SDK 的 P2P 本地会话，未登录或 SDK 查询失败都按空列表处理。
     */
    protected suspend fun queryP2pConversations(): List<Conversation> {
        if (!AccountHelper.isLogged()) return emptyList()
        return try {
            conversationManager.queryConversations()
                .orEmpty()
                .filter { it.conversationType == ConversationType.P2P }
                .filter { it.contactId.isNotBlank() }
                .sortedByDescending { it.time }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 批量补齐会话用户资料，资料失败时保留账号兜底展示。
     */
    protected suspend fun fetchUsers(accounts: List<String>) = try {
        if (accounts.isEmpty()) emptyList() else userManager.fetchUserInfo(accounts).orEmpty()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        emptyList()
    }

    /**
     * 将 IM 会话扩展信息转换成列表展示模型。
     */
    protected fun buildConversationItem(conversation: Conversation, itemType: Int): ConversationItem {
        val account = conversation.contactId
        val profile = conversation.extension[ROLE_HR] as? Map<*, *>
        val profileValue = { key: String -> (profile?.get(key) as? String)?.takeIf { it.isNotBlank() } }
        return ConversationItem(
            id = conversation.conversationId,
            contactId = account,
            itemType = itemType,
            targetName = profileValue(KEY_NAME) ?: conversation.name?.takeIf { it.isNotBlank() } ?: account,
            targetAvatar = profileValue(KEY_AVATAR) ?: conversation.avatar,
            targetCompany = profileValue(KEY_COMPANY),
            targetPosition = profileValue(KEY_POSITION),
            lastMessage = formatLastMessage(conversation),
            lastMessageTime = conversation.time,
            unreadCount = conversation.unreadCount,
            extension = conversation.extension,
        )
    }

    /**
     * 根据会话 serverExtension 判断当前 HR 是否已被求职者标记为不合适。
     */
    protected fun isMarkedUnsuitable(conversation: Conversation): Boolean {
        val hrInfo = conversation.extension[ROLE_HR] as? Map<*, *>
        return hrInfo?.get(KEY_IS_INAPPROPRIATE) == true ||
            conversation.extension[KEY_IS_INAPPROPRIATE] == true
    }

    /**
     * 将 IM 末条消息转换成会话列表摘要，非文本消息展示资源文案。
     */
    protected fun formatLastMessage(conversation: Conversation): String {
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
     * 同步会话列表和空态。
     */
    protected fun refreshMessageList(items: List<MessageListItem>) {
        conversationList.value = items
        isEmpty.set(items.isEmpty())
    }

    /**
     * 绑定 IM 会话列表监听，避免页面恢复时重复注册。
     */
    protected fun registerConversationListener() {
        if (conversationListenerRegistered) return
        conversationManager.addConversationListener(conversationListener)
        conversationListenerRegistered = true
    }

    /**
     * 移除已绑定的 IM 会话监听，避免 ViewModel 销毁后继续持有回调。
     */
    protected fun removeConversationListener() {
        if (!conversationListenerRegistered) return
        conversationManager.removeConversationListener(conversationListener)
        conversationListenerRegistered = false
    }

    /**
     * 取消当前会话加载任务并清空引用，避免旧任务回填列表。
     */
    protected fun cancelLoadJob() {
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

    /**
     * 拉取当前页面需要展示的 IM 会话列表。
     */
    abstract fun getConversationList()

    companion object {
        // 会话扩展中 HR 资料节点字段。
        private const val ROLE_HR = "hr"

        // 会话扩展中求职者资料节点字段。
        private const val ROLE_CANDIDATE = "candidate"

        // 会话扩展中的头像字段。
        private const val KEY_AVATAR = "avatar"

        // 会话扩展中的名称字段。
        private const val KEY_NAME = "name"

        // 会话扩展中的公司字段。
        private const val KEY_COMPANY = "company"

        // 会话扩展中的职位字段。
        private const val KEY_POSITION = "position"

        // 会话扩展中的不合适标记字段。
        private const val KEY_IS_INAPPROPRIATE = "isInappropriate"
    }
}
