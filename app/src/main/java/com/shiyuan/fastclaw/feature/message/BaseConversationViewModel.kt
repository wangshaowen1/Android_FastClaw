package com.shiyuan.fastclaw.feature.message

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.feature.message.data.ConversationItem
import com.shiyuan.fastclaw.feature.message.data.MessageListItem
import com.shiyuan.fastclaw.feature.message.data.MessageDraftStore
import com.tulele.im.ImSession
import com.tulele.im.conversation.ConversationListener
import com.tulele.im.custom.CustomAttachment
import com.tulele.im.entity.Conversation
import com.tulele.im.entity.ConversationType
import com.tulele.im.entity.MessageType
import com.tulele.im.entity.UnknownAttachment
import com.tulele.im.utils.AccountHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
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
    // 当前页面维护的会话缓存，key 为云信 conversationId。
    private val conversationCache = linkedMapOf<String, Conversation>()

    private val conversationListener = object : ConversationListener {
        /**
         * 首次加载会话完成时全量刷新会话缓存。
         */
        override fun onConversationsLoaded(conversations: List<Conversation>?) {
            refreshConversationCache(conversations.orEmpty())
        }

        /**
         * 会话内容发生变化时局部更新会话缓存（一条或多条会话新增 / 更新）。
         */
        override fun onConversationsChanged(conversations: List<Conversation>) {
            updateConversationCache(conversations)
        }

        /**
         * 会话被删除时从缓存移除对应会话。
         */
        override fun onConversationDeleted(conversation: Conversation) {
            removeConversationCache(conversation)
        }

        /**
         * 单条会话的轻量状态刷新（如进入聊天页后未读数归零）。 列表顺序不变，调用方只需局部刷新对应
         */
        override fun onConversationStateChanged(conversation: Conversation) {
            updateConversationCache(listOf(conversation))
        }
    }

    /**
     * ViewModel 创建时先尝试绑定 IM 会话监听，登录完成后页面恢复会再次补注册。
     */
    override fun onCreate() {
        super.onCreate()
        registerConversationListener()
    }

    /**
     * 拉取 IM 的 P2P 会话列表，并刷新当前页面会话缓存。
     */
    fun getConversationList() {
        loadConversationsJob?.cancel()
        loadConversationsJob = viewModelScope.launch {
            isLoading.set(true)
            try {
                if (!AccountHelper.isLogged()) {
                    refreshMessageList(emptyList())
                    return@launch
                }
                val conversations = conversationManager.queryConversations()
                    .orEmpty()
                    .filter { it.conversationType == ConversationType.P2P }
                    .filter { it.contactId.isNotBlank() }
                    .sortedByDescending { it.time }
                refreshConversationCache(conversations)
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
     * 全量刷新会话缓存，并发布当前页面列表。
     */
    protected fun refreshConversationCache(conversations: List<Conversation>) {
        conversationCache.clear()
        updateConversationCache(conversations)
    }

    /**
     * 局部更新会话缓存，并发布当前页面列表。
     */
    protected fun updateConversationCache(conversations: List<Conversation>) {
        conversations.forEach { conversation ->
            if (isValidConversation(conversation)) conversationCache[conversation.conversationId] = conversation
        }
        refreshConversationList()
    }

    /**
     * 从缓存中移除已删除会话，并发布当前页面列表。
     */
    protected fun removeConversationCache(conversation: Conversation) {
        conversationCache.remove(conversation.conversationId)
        refreshConversationList()
    }

    /**
     * 获取当前缓存中的会话列表，按末条消息时间倒序展示。
     */
    protected fun getCachedConversations(): List<Conversation>  {
        return conversationCache.values.sortedByDescending { it.time }
    }

    /**
     * 判断会话是否可以进入当前消息列表缓存。
     */
    private fun isValidConversation(conversation: Conversation): Boolean =
        conversation.conversationType == ConversationType.P2P && conversation.contactId.isNotBlank()

    /**
     * 根据会话 serverExtension 判断当前 HR 是否已被求职者标记为不合适。
     */
    protected fun isMarkedUnsuitable(conversation: Conversation): Boolean {
        val hrInfo = conversation.serverExtension[ROLE_HR] as? Map<*, *>
        return hrInfo?.get(KEY_IS_INAPPROPRIATE) == true
    }

    /**
     * 将 IM 会话扩展信息转换成列表展示模型。
     */
    protected fun buildConversationItem(conversation: Conversation, itemType: Int): ConversationItem {
        val contactId = conversation.contactId
        val hrProfile = conversation.serverExtension[ROLE_HR] as? Map<*, *>
        val hrProfileValue = { key: String -> (hrProfile?.get(key) as? String)?.takeIf { it.isNotBlank() } }
        val position = (conversation.serverExtension[KEY_POSITION] as? String)?.takeIf { it.isNotBlank() }
        val jobId = (conversation.serverExtension[KEY_JOB_ID] as? String)?.takeIf { it.isNotBlank() }
        val draftContent = MessageDraftStore.get(conversation.conversationId)
        return ConversationItem(
            id = conversation.conversationId,
            contactId = contactId,
            itemType = itemType,
            targetName = hrProfileValue(KEY_NAME) ?: conversation.name?.takeIf { it.isNotBlank() } ?: contactId,
            targetAvatar = hrProfileValue(KEY_AVATAR) ?: conversation.avatar,
            targetCompany = hrProfileValue(KEY_COMPANY),
            targetPosition = position,
            jobId = jobId,
            lastMessage = draftContent ?: formatLastMessage(conversation),
            lastMessageTime = conversation.time,
            unreadCount = conversation.unreadCount,
            serverExtension = conversation.serverExtension,
            isDraft = draftContent != null,
        )
    }

    /**
     * 将 IM 末条消息转换成会话列表摘要，自定义消息按协议结构化字段生成文案。
     */
    protected fun formatLastMessage(conversation: Conversation): String {
        if (conversation.messageType == MessageType.custom || conversation.messageType == MessageType.appCustom) {
            val defaultPreview = App.instance.getString(R.string.message_preview_custom)
            val raw = when (val attachment = conversation.attachment) {
                is UnknownAttachment -> attachment.raw
                is CustomAttachment -> attachment.toJson(false)
                else -> return defaultPreview
            }
            val payload = try {
                JSONObject(raw)
            } catch (_: Exception) {
                return defaultPreview
            }
            return when (payload.optString(KEY_TYPE)) {
                TYPE_RESUME_REQUEST -> when (payload.optString(KEY_STATUS)) {
                    STATUS_ACCEPTED, STATUS_SENT -> App.instance.getString(R.string.message_conversation_resume_request_accepted)
                    STATUS_REJECTED -> App.instance.getString(R.string.message_conversation_resume_request_rejected)
                    else -> App.instance.getString(R.string.message_conversation_resume_request_pending)
                }
                TYPE_RESUME_DELIVERY -> {
                    payload.optJSONObject(KEY_RESUME)
                        ?.optString(KEY_NAME)
                        ?.takeIf { it.isNotBlank() }
                        ?: App.instance.getString(R.string.resume_attachment)
                }
                else -> defaultPreview
            }
        }
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
            else -> ""
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
     * 同步会话列表和空态。
     */
    protected fun refreshMessageList(items: List<MessageListItem>) {
        conversationList.value = items
        isEmpty.set(items.isEmpty())
        if (BuildConfig.DEBUG) {
            conversationList.value?.forEachIndexed { index, msg ->
                println("conversationList[$index]=${GsonUtils.toJson(msg)}")
            }
            println("conversationList")
        }
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
     * 根据当前缓存刷新页面自己的消息列表。
     */
    abstract fun refreshConversationList()

    companion object {
        // 会话扩展中 HR 资料节点字段。
        private const val ROLE_HR = "hr"

        // 会话扩展中的头像字段。
        private const val KEY_AVATAR = "avatar"

        // 会话扩展中的名称字段。
        private const val KEY_NAME = "name"

        // 会话扩展中的公司字段。
        private const val KEY_COMPANY = "company"

        // 会话扩展中的职位字段。
        private const val KEY_POSITION = "position"

        // 会话扩展中的岗位 ID 字段。
        private const val KEY_JOB_ID = "jobId"

        // 会话扩展中的后端本地会话业务 ID 字段。
        private const val KEY_SESSION_ID = "sessionId"

        // 会话扩展中的不合适标记字段。
        private const val KEY_IS_INAPPROPRIATE = "isInappropriate"

        // 自定义消息协议类型字段。
        private const val KEY_TYPE = "type"

        // 自定义消息状态字段。
        private const val KEY_STATUS = "status"

        // 自定义消息简历节点字段。
        private const val KEY_RESUME = "resume"

        // HR 索要简历协议类型。
        private const val TYPE_RESUME_REQUEST = "resumeRequest"

        // 求职者主动发送简历协议类型。
        private const val TYPE_RESUME_DELIVERY = "resumeDelivery"

        // 简历消息已同意状态。
        private const val STATUS_ACCEPTED = "accepted"

        // 简历消息已拒绝状态。
        private const val STATUS_REJECTED = "rejected"

        // 旧版本简历消息已发送状态。
        private const val STATUS_SENT = "sent"
    }
}
