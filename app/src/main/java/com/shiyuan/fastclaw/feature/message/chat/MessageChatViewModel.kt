package com.shiyuan.fastclaw.feature.message.chat

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.LogUtils
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.ImFirstMessageNotifyBody
import com.shiyuan.fastclaw.core.network.apis.request.ImJobSwitchBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeSendAgreeBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeSendAgreeBody.Companion.STATUS_ACCEPTED
import com.shiyuan.fastclaw.core.network.apis.request.ResumeSendAgreeBody.Companion.STATUS_REJECTED
import com.shiyuan.fastclaw.core.network.apis.response.ResumeListData
import com.shiyuan.fastclaw.feature.message.MessageFormatter
import com.shiyuan.fastclaw.feature.message.data.ChatMessageItem
import com.shiyuan.fastclaw.feature.message.data.MessageChatParams
import com.shiyuan.fastclaw.feature.message.data.MessageJobAttachment
import com.shiyuan.fastclaw.feature.message.data.MessageTextAttachment
import com.shiyuan.fastclaw.feature.message.data.ResumeDeliveryAttachment
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestAttachment
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestChatItem
import com.tulele.im.Chat
import com.tulele.im.ImCallback
import com.tulele.im.ImSession
import com.tulele.im.entity.Contact
import com.tulele.im.entity.ConversationType
import com.tulele.im.entity.MessageDirection
import com.tulele.im.entity.MessageModifyParams
import com.tulele.im.entity.MessageType
import com.tulele.im.entity.MessageWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// IM 单聊页面 ViewModel，负责会话打开、历史消息加载、实时消息监听和发送文本消息。
class MessageChatViewModel : AppRepositoryViewModel<AppRepository>(), Chat.OnMessageSendResultListener {
    // 当前页面消息列表。
    val messageList = MutableLiveData<List<ChatMessageItem>>(emptyList())
    // 待发送的最新简历，页面收到后弹出发送确认框。
    val resumeForDelivery = MutableLiveData<ResumeListData?>()
    // 当前是否正在加载更早历史消息。
    val isLoadingHistory = ObservableBoolean(false)

    // 当前打开的 IM 会话门面。
    private var chat: Chat? = null
    // 当前会话消息缓存，key 为 messageClientId。
    private val messageCache = linkedMapOf<String, MessageWrapper>()
    // 本机主动发送过的消息 id，用于 SDK 回填方向前保持右侧展示。
    private val localSendMessageIds = mutableSetOf<String>()
    // 本机主动发送消息的展示顺序时间，避免连续发送多条消息时 SDK 回填顺序抖动。
    private val localSendMessageTimes = mutableMapOf<String, Long>()
    // 下一条本机主动发送消息的展示顺序时间。
    private var nextLocalSendMessageTime = 0L
    // 是否还有更早历史消息。
    private var hasMoreHistory = true
    // 当前后端本地会话业务 ID。
    private var sessionId: String = ""
    // 当前聊天对象账号。
    private var contactId: String = ""
    // 当前聊天对象头像。
    private var targetAvatar: String? = null
    // 首次创建会话时是否需要自动发送开场白。
    private var autoSendGreeting: Boolean? = false

    /**
     * 初始化聊天参数、打开当前 IM 会话并加载历史消息。
     * 首次创建会话时是否自动发送开场白。
     */
    fun initChat(params: MessageChatParams?) {
        this.sessionId = params?.sessionId.orEmpty()
        this.contactId = params?.contactId.orEmpty()
        this.targetAvatar = params?.avatar
        this.autoSendGreeting = params?.isFirstCreate
        chat = ImSession.get().openChat(Contact(contactId))
        chat?.onMessageSendResultListener = this
        chat?.active()
        clearUnreadMessage(contactId)
        fetchTargetAvatar(contactId)
        initObserve()
        loadDataList()
    }

    /**
     * 监听当前会话实时消息变化。
     */
    fun initObserve() {
        val currentChat = chat ?: return
        viewModelScope.launch {
            currentChat.observeMessages().collectLatest { messages ->
                LogUtils.d("监听收到的消息", JSON.toJSONString(messages))
                mergeMessages(messages)
                clearUnreadReceiving(messages)
            }
        }
        viewModelScope.launch {
            currentChat.observeModifiedMessages().collectLatest { messages ->
                mergeMessages(messages)
            }
        }
    }

    /**
     * 首次加载最近历史消息，必要时发送首次创建开场白。
     */
    fun loadDataList() {
        val currentChat = chat ?: return
        isLoadingHistory.set(true)
        currentChat.queryHistory(null, true, object : ImCallback<List<MessageWrapper>> {
            /**
             * 首次历史消息加载成功时刷新当前消息缓存。
             */
            override fun onSuccess(result: List<MessageWrapper>?) {
                val messages = result.orEmpty().filterNot { it.shouldIgnore() }
                messageCache.clear()
                messages.forEach { message ->
                    messageCache[message.messageClientId] = message
                }
                hasMoreHistory = messages.size >= Chat.LOAD_MESSAGE_COUNT
                refreshMessageList()
                isLoadingHistory.set(false)
                onLoadDataComplete()
            }

            /**
             * 首次历史消息加载失败时结束加载状态。
             */
            override fun onFailed(code: Int) {
                isLoadingHistory.set(false)
                onLoadDataComplete()
            }

            /**
             * 首次历史消息加载异常时结束加载状态。
             */
            override fun onException(exception: Throwable) {
                isLoadingHistory.set(false)
                onLoadDataComplete()
            }
        })
    }

    /**
     * 加载更早历史消息，页面滚动接近顶部时调用。
     */
    fun loadMoreList(): Boolean {
        val anchor = messageCache.values.minByOrNull { it.time } ?: return false
        val currentChat = chat ?: return false
        if (isLoadingHistory.get() || !hasMoreHistory) return false
        isLoadingHistory.set(true)
        currentChat.queryHistory(anchor, true, object : ImCallback<List<MessageWrapper>> {
            /**
             * 加载更早历史成功时追加到当前消息缓存。
             */
            override fun onSuccess(result: List<MessageWrapper>?) {
                val messages = result.orEmpty().filterNot { it.shouldIgnore() }
                val newMessages = messages.filterNot { messageCache.containsKey(it.messageClientId) }
                newMessages.forEach { message ->
                    messageCache[message.messageClientId] = message
                }
                hasMoreHistory = newMessages.isNotEmpty() && messages.size >= Chat.LOAD_MESSAGE_COUNT
                refreshMessageList()
                isLoadingHistory.set(false)
            }

            /**
             * 加载更早历史失败时结束加载状态。
             */
            override fun onFailed(code: Int) {
                isLoadingHistory.set(false)
            }

            /**
             * 加载更早历史异常时结束加载状态。
             */
            override fun onException(exception: Throwable) {
                isLoadingHistory.set(false)
            }
        })
        return true
    }

    /**
     * 首次加载数据完成
     */
    private fun onLoadDataComplete() {
        if (autoSendGreeting == true) {
            autoSendGreeting = false
            sendTextMessage(FIRST_CREATE_GREETING)
        }
    }

    /**
     * 求职者发送文本消息 - 并立即把本地消息插入列表。
     */
    fun sendTextMessage(content: String) {
        val text = content.trim().takeIf { it.isNotBlank() } ?: return
        val isFirstReply = isFirstReply()
        val message = chat?.sendTextMessage(text) ?: return
        rememberLocalSendMessage(message)
        mergeMessages(listOf(message))
        if (isFirstReply) {
            notifyFirstMessageSent()
        }
    }

    /**
     * 求职者主动发送简历 - 组装 resumeDelivery 消息并即时插入列表。
     */
    fun sendResumeDeliveryMessage(resume: ResumeListData) {
        val currentChat = chat ?: return
        val resumeName = resume.resumeName.orEmpty().ifBlank { resume.fileName.orEmpty() }
        if (resume.resumeId.isNullOrBlank() || resumeName.isBlank() || resume.resumeUrl.isNullOrBlank()) {
            showToast(R.string.message_chat_resume_empty_tip)
            return
        }
        val textMessage = currentChat.sendTextMessage(RESUME_DELIVERY_TEXT)
        rememberLocalSendMessage(textMessage)
        val attachment = ResumeDeliveryAttachment.buildResume(resume)
        val message = currentChat.createCustomMessage(attachment)
        message.setContent(resumeName)
        rememberLocalSendMessage(message)
        currentChat.reSendMessage(message)
        mergeMessages(listOf(textMessage, message))
    }

    /**
     * 记录本机主动发送消息的展示顺序，避免同一批消息按 SDK 回填时间互换位置。
     */
    private fun rememberLocalSendMessage(message: MessageWrapper) {
        localSendMessageIds.add(message.messageClientId)
        val sortTime = maxOf(System.currentTimeMillis(), nextLocalSendMessageTime + 1)
        nextLocalSendMessageTime = sortTime
        localSendMessageTimes[message.messageClientId] = sortTime
    }

    /**
     * HR请求简历 - 按新的简历请求附件内容修改原消息。
     */
    private fun modifyResumeRequestMessage(id: String, attachment: ResumeRequestAttachment, onSuccess: () -> Unit = {}, ) {
        val currentChat = chat ?: return
        val message = messageCache[id] ?: return
        val params = MessageModifyParams(
            text = attachment.pushContent.toString(),
            customAttachment = attachment,
        )
        currentChat.modifyMessage(message, params, object : ImCallback<MessageWrapper> {
            //云端消息修改成功后刷新当前消息。
            override fun onSuccess(result: MessageWrapper?) {
                mergeMessages(listOfNotNull(result))
                onSuccess()
            }

            //云端消息修改失败时提示错误码。
            override fun onFailed(code: Int) {
                showToast(R.string.message_chat_resume_request_failed)
            }

            //云端消息修改异常时提示失败。
            override fun onException(exception: Throwable) {
                showToast(R.string.message_chat_resume_request_failed)
            }
        })
    }

    /**
     * 文本消息发送成功时刷新对应消息状态。
     */
    override fun onSendSuccess(message: MessageWrapper, antispamResult: String?) {
        mergeMessages(listOf(message))
    }

    /**
     * 文本消息发送失败时刷新对应消息状态。
     */
    override fun onSendFailed(message: MessageWrapper, errorCode: Int, throwable: Throwable?) {
        mergeMessages(listOf(message))
    }

    /**
     * 合并新增或变化的消息，并按消息时间重新发布列表。
     */
    private fun mergeMessages(messages: List<MessageWrapper>) {
        messages.filterNot { it.shouldIgnore() }.forEach { message ->
            messageCache[message.messageClientId] = message
        }
        refreshMessageList()
    }

    /**
     * 判断消息是否需要在聊天列表中忽略。
     */
    private fun MessageWrapper.shouldIgnore(): Boolean {
        return isDeleted || (messageType == MessageType.custom && subtype == SUBTYPE_RESUME_REQUEST_RESULT)
    }

    /**
     * 判断是回复对方的第一条消息
     */
    private fun isFirstReply() : Boolean {
        return messageCache.values.none { message -> message.direct == MessageDirection.Out && !message.shouldIgnore() }
    }

    /**
     * 将消息缓存转换成页面列表模型。
     */
    private fun refreshMessageList() {
        // 本地即时上屏消息优先使用本地排序时间，避免连续发送时 SDK 回填时间导致顺序抖动。
        val messages = messageCache.values.sortedBy { localSendMessageTimes[it.messageClientId] ?: it.time }
        // 构建页面 item 时顺带根据上一条消息计算时间标签，超过间隔才展示时间。
        val items = messages.mapIndexed { index, message ->
            buildMessageItem(message, buildMessageTime(messages.getOrNull(index - 1), message))
        }
        // 内容没有变化时不重复发布，减少 RecyclerView 无意义刷新。
        if (messageList.value == items) return
        messageList.value = items
    }

    /**
     * 从 IM 用户资料中查询一次对方头像，查询成功后刷新当前消息头像。
     */
    private fun fetchTargetAvatar(contactId: String) {
        viewModelScope.launch {
            try {
                val avatar = ImSession.get().getUserManager()
                    .fetchUserInfo(contactId)
                    ?.avatar
                    ?.takeIf { it.isNotBlank() }
                    ?: return@launch
                if (targetAvatar == avatar) return@launch
                targetAvatar = avatar
                refreshMessageList()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 查询最新可用简历，成功后通知页面弹出确认框。
     */
    fun queryResumeForDelivery() {
        requestWithLoading(apiBlock = { mRepo.api.resumeQuery(PagedBody(1, 100)) },
            showLoading = false,
            showToastError = true,
        ) {
            resumeForDelivery.value = data?.items.orEmpty()
                .filter { it.parseStatus == RESUME_PARSE_STATUS_SUCCESS }
                .maxByOrNull { it.uploadTime.orEmpty() }
        }
    }

    /**
     * 同意 HR 索要简历请求，先告诉后台。
     */
    fun acceptResumeForRequest(sessionId: String, item: ResumeRequestChatItem) {
        handleResumeForRequest(sessionId, item, STATUS_ACCEPTED) {
            sendTextMessage(RESUME_REQUEST_ACCEPT_TEXT)
        }
    }

    /**
     * 拒绝 HR 索要简历请求，先告诉后台。
     */
    fun rejectResumeForRequest(sessionId: String, item: ResumeRequestChatItem) {
        handleResumeForRequest(sessionId, item, STATUS_REJECTED)
    }

    /**
     * 提交 HR 索要简历处理结果，由后台负责更新原 IM 消息。
     */
    fun handleResumeForRequest(sessionId: String, item: ResumeRequestChatItem, status: String, onSuccess: () -> Unit = {}, ) {
        if (item.messageServerId.isNullOrBlank()) {
            showToast(R.string.message_chat_resume_request_failed)
            return
        }
        requestWithLoading(apiBlock = {
            mRepo.api.agreeResumeSend(ResumeSendAgreeBody(
                    sessionId = sessionId,
                    messageServerId = item.messageServerId,
                    status = status,
                )
            )
        }, showLoading = false, showToastError = true) {
            onSuccess()
        }
    }

    /**
     * 调用后端接口切换当前 IM 会话沟通职位。
     */
    fun switchTargetJob(currentJobId: String?, onSuccess: () -> Unit = {}) {
        currentJobId ?: return
        requestWithLoading(apiBlock = {
            mRepo.api.switchImJob(ImJobSwitchBody(currentJobId))
        }) {
            onSuccess()
        }
    }

    /**
     * 通知后端当前会话首条聊天消息已发送成功。
     */
    fun notifyFirstMessageSent() {
        requestWithLoading(
            apiBlock = { mRepo.api.notifyFirstMessageSent(ImFirstMessageNotifyBody(sessionId)) },
            showLoading = false,
            showToastError = false,
        ) {}
    }

    /**
     * 根据云信消息类型转换成对应的页面消息模型。
     */
    private fun buildMessageItem(message: MessageWrapper, timeText: String): ChatMessageItem {
        val isSelf = message.direct == MessageDirection.Out || message.messageClientId in localSendMessageIds
        return when {
            // HR 索要简历消息，按协议 attachment.raw 解析请求状态和简历信息。
            message.messageType == MessageType.custom && message.subtype == SUBTYPE_RESUME_REQUEST -> {
                ResumeRequestAttachment.buildChat(message, isSelf, timeText, messageAvatar(isSelf))
            }
            // 求职者主动发送简历消息，按协议 attachment.raw 解析投递状态和简历文件。
            message.messageType == MessageType.custom && message.subtype == SUBTYPE_RESUME_DELIVERY -> {
                ResumeDeliveryAttachment.buildChat(message, isSelf, timeText)
            }
            // 职业卡片消息，按协议 attachment.raw.job 解析职位快照字段。
            message.messageType == MessageType.custom && message.subtype == SUBTYPE_JOB_CARD -> {
                MessageJobAttachment.buildChat(message, timeText)
            }
            // 普通文本或暂不支持的消息类型，兜底展示为文本气泡。
            else -> {
                MessageTextAttachment.buildChat(message, isSelf, timeText, messageAvatar(isSelf))
            }
        }
    }

    /**
     * 按消息间隔规则生成时间文案，首条消息或间隔超过 5 分钟才展示。
     */
    private fun buildMessageTime(previous: MessageWrapper?, current: MessageWrapper): String {
        val shouldShowTime = previous == null || current.time - previous.time > MESSAGE_TIME_INTERVAL
        return if (shouldShowTime) MessageFormatter.format(current.time) else ""
    }

    /**
     * 根据消息方向返回当前用户或聊天对象头像。
     */
    private fun messageAvatar(isSelf: Boolean): String? {
        return if (isSelf) User.get().avatarUrl else targetAvatar
    }

    /**
     * 当前聊天页收到对方新消息后清空会话未读数。
     */
    private fun clearUnreadReceiving(messages: List<MessageWrapper>) {
        // 新接收的任意一条消息是收到的消息，也就是对方发来的
        val hasIncomingMessage = messages.any { message -> message.direct == MessageDirection.In }
        if (hasIncomingMessage) clearUnreadMessage(contactId)
    }

    /**
     * 清空当前单聊会话未读数，避免用户已进入聊天页后列表和首页仍展示红点。
     */
    private fun clearUnreadMessage(contactId: String) {
        ImSession.get().getConversationManager().markConversationRead(contactId, ConversationType.P2P)
    }

    /**
     * ViewModel 销毁时关闭当前 IM 会话并取消监听任务。
     */
    override fun onDestroy() {
        chat?.close()
        chat = null
        super.onDestroy()
    }

    companion object {
        // 简历请求自定义消息 subtype。
        private const val SUBTYPE_RESUME_REQUEST = 1001

        // 简历投递自定义消息 subtype。
        private const val SUBTYPE_RESUME_DELIVERY = 1002

        // 职位卡片自定义消息 subtype。
        private const val SUBTYPE_JOB_CARD = 1003

        // 简历请求结果消息 subtype，只过滤不展示。
        private const val SUBTYPE_RESUME_REQUEST_RESULT = 1004

        // 简历解析成功状态。
        private const val RESUME_PARSE_STATUS_SUCCESS = 2

        // 相邻消息超过 5 分钟时重新展示时间。
        private const val MESSAGE_TIME_INTERVAL = 5 * 60 * 1000L

        // 主动发送简历前先发送的文本消息内容。
        private const val RESUME_DELIVERY_TEXT = "这是我的简历，您看看"

        // 同意 HR 索要简历后发送的文本消息内容。
        private const val RESUME_REQUEST_ACCEPT_TEXT = "您好，我已投递简历"

        // 首次创建会话时自动发送的开场白。
        private const val FIRST_CREATE_GREETING = "您好，认真看了岗位介绍，很感兴趣，可以深入聊聊吗？"

    }
}
