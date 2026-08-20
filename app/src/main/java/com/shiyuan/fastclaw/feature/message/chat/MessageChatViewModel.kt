package com.shiyuan.fastclaw.feature.message.chat

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.feature.message.MessageTimeFormatter
import com.shiyuan.fastclaw.feature.message.data.ChatMessageItem
import com.shiyuan.fastclaw.feature.message.data.JobChatItem
import com.shiyuan.fastclaw.feature.message.data.ResumeDeliveryChatItem
import com.shiyuan.fastclaw.feature.message.data.ResumeRequestChatItem
import com.shiyuan.fastclaw.feature.message.data.TextChatItem
import com.tulele.im.Chat
import com.tulele.im.ImCallback
import com.tulele.im.ImSession
import com.tulele.im.custom.CustomAttachment
import com.tulele.im.entity.Contact
import com.tulele.im.entity.MessageDirection
import com.tulele.im.entity.MessageType
import com.tulele.im.entity.MessageWrapper
import com.tulele.im.entity.UnknownAttachment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

// IM 单聊页面 ViewModel，负责会话打开、历史消息加载、实时消息监听和发送文本消息。
class MessageChatViewModel : AppRepositoryViewModel<AppRepository>(), Chat.OnMessageSendResultListener {
    // 当前页面消息列表。
    val messageList = MutableLiveData<List<ChatMessageItem>>(emptyList())
    // 当前是否正在加载更早历史消息。
    val isLoadingHistory = ObservableBoolean(false)

    // 当前打开的 IM 会话门面。
    private var chat: Chat? = null
    // 当前会话消息缓存，key 为 messageClientId。
    private val messageCache = linkedMapOf<String, MessageWrapper>()
    // 是否还有更早历史消息。
    private var hasMoreHistory = true

    /**
     * 初始化聊天参数、打开当前 IM 会话并加载历史消息。
     */
    fun initChat(contactId: String) {
        chat = ImSession.get().openChat(Contact(contactId))
        chat?.onMessageSendResultListener = this
        chat?.active()
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
                mergeMessages(messages)
            }
        }
    }

    /**
     * 首次加载最近历史消息。
     */
    fun loadDataList() {
        val currentChat = chat ?: return
        isLoadingHistory.set(true)
        currentChat.queryHistory(null, true, object : ImCallback<List<MessageWrapper>> {
            /**
             * 首次历史消息加载成功时刷新当前消息缓存。
             */
            override fun onSuccess(result: List<MessageWrapper>?) {
                val messages = result.orEmpty().filterNot { it.isDeleted }
                messageCache.clear()
                messages.forEach { message ->
                    messageCache[message.messageClientId] = message
                }
                hasMoreHistory = messages.size >= Chat.LOAD_MESSAGE_COUNT
                refreshMessageList()
                isLoadingHistory.set(false)
            }

            /**
             * 首次历史消息加载失败时结束加载状态。
             */
            override fun onFailed(code: Int) {
                isLoadingHistory.set(false)
            }

            /**
             * 首次历史消息加载异常时结束加载状态。
             */
            override fun onException(exception: Throwable) {
                isLoadingHistory.set(false)
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
                val messages = result.orEmpty().filterNot { it.isDeleted }
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
     * 发送文本消息，并立即把本地消息插入列表。
     */
    fun sendTextMessage(content: String) {
        val text = content.trim()
        if (text.isBlank()) return
        val message = chat?.sendTextMessage(text) ?: return
        mergeMessages(listOf(message))
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
        messages.filterNot { it.isDeleted }.forEach { message ->
            messageCache[message.messageClientId] = message
        }
        refreshMessageList()
    }

    /**
     * 将消息缓存转换成页面列表模型。
     */
    private fun refreshMessageList() {
        val messages = messageCache.values.sortedBy { it.time }
        val items = messages.mapIndexed { index, message ->
            buildMessageItem(message, buildMessageTime(messages.getOrNull(index - 1), message))
        }
        if (messageList.value == items) return
        messageList.value = items
    }

    /**
     * 根据云信消息类型转换成对应的页面消息模型。
     */
    private fun buildMessageItem(message: MessageWrapper, timeText: String): ChatMessageItem {
        val isSelf = message.direct == MessageDirection.Out
        val payload = message.customPayload()
        return when {
            // HR 索要简历消息，按协议 attachment.raw 解析请求状态和简历信息。
            message.messageType == MessageType.custom && message.subtype == SUBTYPE_RESUME_REQUEST -> {
                val resume = payload?.optJSONObject("resume")
                val status = payload?.optString("status")
                ResumeRequestChatItem(
                    id = message.messageClientId,
                    protocolVersion = payload?.optInt("v"),
                    protocolType = payload?.optString("type"),
                    requestId = payload?.optString("requestId"),
                    status = status,
                    title = App.instance.getString(R.string.message_chat_resume_request_title),
                    statusText = status.resumeStatusText(),
                    resumeId = resume?.optString("resumeId"),
                    resumeName = resume?.optString("name"),
                    resumeUrl = resume?.optString("url"),
                    resumeMimeType = resume?.optString("mimeType"),
                    resumeSize = resume?.optLong("size"),
                    createdAt = payload?.optLong("createdAt"),
                    updatedAt = payload?.optLong("updatedAt"),
                    operatorId = payload?.optString("operatorId"),
                    isSelf = isSelf,
                    time = message.time,
                    timeText = timeText,
                )
            }
            // 求职者主动发送简历消息，按协议 attachment.raw 解析投递状态和简历文件。
            message.messageType == MessageType.custom && message.subtype == SUBTYPE_RESUME_DELIVERY -> {
                val resume = payload?.optJSONObject("resume")
                val status = payload?.optString("status")
                ResumeDeliveryChatItem(
                    id = message.messageClientId,
                    protocolVersion = payload?.optInt("v"),
                    protocolType = payload?.optString("type"),
                    deliveryId = payload?.optString("deliveryId"),
                    status = status,
                    title = App.instance.getString(R.string.message_chat_resume_delivery_title),
                    fileName = resume?.optString("name")
                        ?: App.instance.getString(R.string.message_chat_resume_file_default),
                    statusText = status.resumeStatusText(),
                    resumeId = resume?.optString("resumeId"),
                    resumeUrl = resume?.optString("url"),
                    resumeMimeType = resume?.optString("mimeType"),
                    resumeSize = resume?.optLong("size"),
                    createdAt = payload?.optLong("createdAt"),
                    updatedAt = payload?.optLong("updatedAt"),
                    operatorId = payload?.optString("operatorId"),
                    isSelf = isSelf,
                    time = message.time,
                    timeText = timeText,
                )
            }
            // 职业卡片消息，按协议 attachment.raw.job 解析职位快照字段。
            message.messageType == MessageType.custom && message.subtype == SUBTYPE_JOB_CARD -> {
                val job = payload?.optJSONObject("job")
                JobChatItem(
                    id = message.messageClientId,
                    protocolVersion = payload?.optInt("v"),
                    protocolType = payload?.optString("type"),
                    cardId = payload?.optString("cardId"),
                    jobId = job?.optString("jobId"),
                    jobName = job?.optString("jobName"),
                    salaryText = job?.optString("salaryText"),
                    brandId = job?.optString("brandId"),
                    brandName = job?.optString("brandName"),
                    companyId = job?.optString("companyId"),
                    companyName = job?.optString("companyName"),
                    cityCode = job?.optString("cityCode"),
                    cityName = job?.optString("cityName"),
                    experienceCode = job?.optString("experienceCode"),
                    experienceText = job?.optString("experienceText"),
                    educationCode = job?.optString("educationCode"),
                    educationText = job?.optString("educationText"),
                    isJobChanged = job?.optBoolean("isJobChanged") == true,
                    time = message.time,
                    timeText = timeText,
                )
            }
            // 普通文本或暂不支持的消息类型，兜底展示为文本气泡。
            else -> {
                TextChatItem(
                    id = message.messageClientId,
                    content = message.content?.takeIf { it.isNotBlank() }
                        ?: App.instance.getString(R.string.message_preview_custom),
                    avatar = if (isSelf) User.get().avatarUrl else null,
                    isSelf = isSelf,
                    time = message.time,
                    timeText = timeText,
                )
            }
        }
    }

    /**
     * 按消息间隔规则生成时间文案，首条消息或间隔超过 5 分钟才展示。
     */
    private fun buildMessageTime(previous: MessageWrapper?, current: MessageWrapper): String {
        val shouldShowTime = previous == null || current.time - previous.time > MESSAGE_TIME_INTERVAL
        return if (shouldShowTime) MessageTimeFormatter.format(current.time) else ""
    }

    /**
     * 读取协议自定义消息附件 JSON。
     */
    private fun MessageWrapper.customPayload(): JSONObject? {
        val raw = when (val attachment = attachment) {
            is UnknownAttachment -> attachment.raw
            is CustomAttachment -> attachment.toJson(false)
            else -> null
        }?.takeIf { it.isNotBlank() } ?: return null
        return try {
            JSONObject(raw)
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * 将简历请求状态转换成展示文案。
     */
    private fun String?.resumeStatusText(): String = when (this) {
        STATUS_ACCEPTED -> App.instance.getString(R.string.message_chat_resume_delivery_sent)
        STATUS_REJECTED -> App.instance.getString(R.string.message_chat_resume_request_rejected)
        STATUS_PENDING -> App.instance.getString(R.string.message_chat_resume_request_pending)
        STATUS_SENT -> App.instance.getString(R.string.message_chat_resume_delivery_sent)
        else -> App.instance.getString(R.string.message_chat_resume_request_pending)
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

        // 相邻消息超过 5 分钟时重新展示时间。
        private const val MESSAGE_TIME_INTERVAL = 5 * 60 * 1000L

        // 简历请求待处理状态。
        private const val STATUS_PENDING = "pending"

        // 简历请求已同意状态。
        private const val STATUS_ACCEPTED = "accepted"

        // 简历请求已拒绝状态。
        private const val STATUS_REJECTED = "rejected"

        // 简历投递已发送状态。
        private const val STATUS_SENT = "sent"
    }
}
