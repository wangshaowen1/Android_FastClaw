package com.shiyuan.fastclaw.feature.clawagent

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.network.apis.request.ActionSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.AttributeConfirmBody
import com.shiyuan.fastclaw.core.network.apis.request.AttributeRejectBody
import com.shiyuan.fastclaw.core.network.apis.request.ConversationQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CurrentResumeQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.JobAgentCancelBody
import com.shiyuan.fastclaw.core.network.apis.request.JobAgentStreamBody
import com.shiyuan.fastclaw.core.network.apis.request.MessagePageBody
import com.shiyuan.fastclaw.core.network.apis.request.MessagePullBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeConfirmBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.SessionEnterBody
import com.shiyuan.fastclaw.core.network.apis.response.ActionSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.ConversationQueryData
import com.shiyuan.fastclaw.core.network.apis.response.CurrentResumeData
import com.shiyuan.fastclaw.core.network.apis.response.MessagePageData
import com.shiyuan.fastclaw.core.network.apis.response.MessagePullData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeConfirmData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.SessionEnterData
import com.shiyuan.fastclaw.core.network.sse.RawSSEStreamHandler
import com.shiyuan.fastclaw.core.network.sse.processor.PlainTextSseProcessor
import java.util.UUID

/**
 * @createtime 2026年05月20日 10:27
 * @author TooWhiteT
 */

class ClawAgentViewModel : AppRepositoryViewModel<AppRepository>() {

    /** 流式输出的累积文本 */
    val streamingText = MutableLiveData<String>()

    /** 流是否正在输出中 */
    val streaming = MutableLiveData<Boolean>(false)

    /** 流式错误（一次性） */
    val streamError = MutableLiveData<Throwable?>()

    /** 进入/恢复会话返回的数据（会话状态 + 历史消息） */
    val sessionData = MutableLiveData<SessionEnterData?>()

    /** 查询通信会话返回的数据（实例/对话/流式请求状态） */
    val conversationData = MutableLiveData<ConversationQueryData?>()

    /** 提交卡片/按钮动作返回的数据（含本次动作产生或更新的消息） */
    val actionResult = MutableLiveData<ActionSubmitData?>()

    /** 历史消息分页结果（向上翻页加载更早消息） */
    val historyMessages = MutableLiveData<MessagePageData?>()

    /** 增量消息拉取结果（拉取游标之后的新消息） */
    val pulledMessages = MutableLiveData<MessagePullData?>()

    /** 当前可确认简历摘要 */
    val currentResume = MutableLiveData<CurrentResumeData?>()

    /** 确认/否认当前简历的结果 */
    val resumeConfirmResult = MutableLiveData<ResumeConfirmData?>()

    /** 提交简历上传结果的返回 */
    val resumeUploadResult = MutableLiveData<ResumeUploadSubmitData?>()

    /** 确认属性候选值的结果（复用动作响应 envelope，含产生的消息） */
    val attributeConfirmResult = MutableLiveData<ActionSubmitData?>()

    /** 拒绝属性候选值的结果（复用动作响应 envelope，含产生的消息） */
    val attributeRejectResult = MutableLiveData<ActionSubmitData?>()

    /** 当前会话 ID，发送消息/提交动作时回传 */
    private var sessionId: String? = null

    /** 当前对话 ID */
    private var conversationId: String? = null

    /** 已持有的最新消息游标，用于增量恢复 */
    private var latestCursor: String? = null

    private val sseProcessor by lazy {
        PlainTextSseProcessor(
            scope = viewModelScope,
            onStart = {
                streaming.value = true
                streamError.value = null
                streamingText.value = ""
            },
            onUpdate = { content ->
                streamingText.value = content
            },
            onComplete = {
                streaming.value = false
            },
            onErrorCallback = { e ->
                streaming.value = false
                streamError.value = e
            }
        )
    }

    private val sseHandler by lazy {
        RawSSEStreamHandler(viewModelScope, sseProcessor)
    }

    /**
     * 进入或恢复会话：sessionId 为空表示首次进入，由服务端恢复最新会话
     */
    fun enterSession() {
        request(
            apiBlock = {
                mRepo.api.enterSession(
                    SessionEnterBody(
                        sessionId = sessionId,
                        conversationId = conversationId,
                        clientRequestId = UUID.randomUUID().toString(),
                        latestCursor = latestCursor,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data ?: return@request
            // 记录会话上下文，供后续发送消息/增量恢复使用
            sessionId = result.sessionId
            conversationId = result.conversationId
            latestCursor = result.latestCursor
            sessionData.value = result
        }
    }

    /**
     * 确认属性候选值：questionQueueId 来自属性确认卡（ATTRIBUTE_CONFIRM）所在消息。
     * 复用缓存的会话上下文，成功后回写 latestCursor；reason 为可选的拒绝原因或确认备注。
     */
    fun confirmAttribute(questionQueueId: String, reason: String? = null) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        request(
            apiBlock = {
                mRepo.api.confirmAttribute(
                    AttributeConfirmBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        clientActionId = UUID.randomUUID().toString(),
                        questionQueueId = questionQueueId,
                        reason = reason,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data ?: return@request
            result.latestCursor?.let { latestCursor = it }
            attributeConfirmResult.value = result
        }
    }

    /**
     * 拒绝属性候选值：questionQueueId 来自属性确认卡（ATTRIBUTE_CONFIRM）所在消息。
     * 复用缓存的会话上下文，成功后回写 latestCursor；reason 为可选的拒绝原因。
     */
    fun rejectAttribute(questionQueueId: String, reason: String? = null) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        request(
            apiBlock = {
                mRepo.api.rejectAttribute(
                    AttributeRejectBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        clientActionId = UUID.randomUUID().toString(),
                        questionQueueId = questionQueueId,
                        reason = reason,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data ?: return@request
            result.latestCursor?.let { latestCursor = it }
            attributeRejectResult.value = result
        }
    }

    /**
     * 提交简历上传结果：端侧先完成文件上传后，回传 resumeId / resumeTaskId / resumeFileUrl 之一衔接解析。
     * 复用缓存的会话上下文，成功后回写 latestCursor。
     */
    fun submitResumeUpload(
        resumeId: String? = null,
        resumeTaskId: String? = null,
        resumeFileUrl: String? = null,
    ) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        request(
            apiBlock = {
                mRepo.api.submitResumeUpload(
                    ResumeUploadSubmitBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        clientActionId = UUID.randomUUID().toString(),
                        resumeId = resumeId,
                        resumeTaskId = resumeTaskId,
                        resumeFileUrl = resumeFileUrl,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data ?: return@request
            result.latestCursor?.let { latestCursor = it }
            resumeUploadResult.value = result
        }
    }

    /**
     * 确认或否认当前历史简历是否本人当前可用简历。
     * resumeId 来自 queryCurrentResume / enterSession；复用缓存的会话上下文，成功后回写 latestCursor。
     */
    fun confirmCurrentResume(resumeId: String, confirmed: Boolean) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        request(
            apiBlock = {
                mRepo.api.confirmCurrentResume(
                    ResumeConfirmBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        clientActionId = UUID.randomUUID().toString(),
                        resumeId = resumeId,
                        confirmed = confirmed,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data ?: return@request
            result.latestCursor?.let { latestCursor = it }
            resumeConfirmResult.value = result
        }
    }

    /**
     * 查询当前可确认简历：sessionId 为空时只按当前登录候选人查询可确认简历摘要（可在进入会话前调用）。
     */
    fun queryCurrentResume() {
        request(
            apiBlock = {
                mRepo.api.queryCurrentResume(CurrentResumeQueryBody(sessionId = sessionId))
            },
            showToast = true,
        ) {
            currentResume.value = this.data
        }
    }

    /**
     * 拉取增量消息：服务端返回游标之后的新消息。
     * afterCursor 为空时默认用本地缓存的 latestCursor；成功后回写 latestCursor 供下次增量拉取。
     */
    fun pullMessages(afterCursor: String? = null, limit: Int? = null) {
        val currentSessionId = sessionId
        if (currentSessionId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        request(
            apiBlock = {
                mRepo.api.pullMessages(
                    MessagePullBody(
                        sessionId = currentSessionId,
                        afterCursor = afterCursor ?: latestCursor,
                        limit = limit,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data ?: return@request
            result.latestCursor?.let { latestCursor = it }
            pulledMessages.value = result
        }
    }

    /**
     * 查询历史消息分页（向上翻页加载更早消息）。
     * beforeCursor 为空表示从当前最新消息之前查询；用返回的 nextBeforeCursor + hasMore 继续翻页。
     */
    fun loadHistoryMessages(beforeCursor: String? = null, pageSize: Int? = null) {
        val currentSessionId = sessionId
        if (currentSessionId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        request(
            apiBlock = {
                mRepo.api.queryMessagePage(
                    MessagePageBody(
                        sessionId = currentSessionId,
                        beforeCursor = beforeCursor,
                        pageSize = pageSize,
                    )
                )
            },
            showToast = true,
        ) {
            historyMessages.value = this.data
        }
    }

    /**
     * 查询通信会话：仅查询当前候选人的求职 Agent 实例、活跃对话和流式请求状态，不建立 SSE 连接。
     * requestId 为空查询最近一次请求；sceneType 见接口枚举（1 / 2）。查询结果会同步本地缓存的会话上下文。
     */
    fun queryConversation(requestId: String? = null, sceneType: Int? = null) {
        request(
            apiBlock = {
                mRepo.api.queryConversation(
                    ConversationQueryBody(
                        conversationId = conversationId,
                        sessionId = sessionId,
                        requestId = requestId,
                        sceneType = sceneType,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data ?: return@request
            // 同步会话上下文，供发送消息/取消/增量恢复复用
            result.sessionId?.let { sessionId = it }
            result.conversationId?.let { conversationId = it }
            result.latestCursor?.let { latestCursor = it }
            conversationData.value = result
        }
    }

    /**
     * 提交卡片或按钮动作（如点击上传简历入口、确认/否认历史简历、跳过问题、确认属性等）。
     * 复用 enterSession/queryConversation 缓存的会话上下文；actionCode 见 [ActionSubmitBody] 白名单。
     * @param messageId         动作来源消息 ID（卡片/按钮所在消息）
     * @param questionQueueId   动作来源问题队列 ID
     * @param actionPayloadJson 动作扩展 JSON（按钮值、确认值等）
     */
    fun submitAction(
        actionCode: String,
        messageId: String? = null,
        questionQueueId: String? = null,
        actionPayloadJson: String? = null,
    ) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        request(
            apiBlock = {
                mRepo.api.submitAction(
                    ActionSubmitBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        clientActionId = UUID.randomUUID().toString(),
                        actionCode = actionCode,
                        messageId = messageId,
                        questionQueueId = questionQueueId,
                        actionPayloadJson = actionPayloadJson,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data ?: return@request
            result.latestCursor?.let { latestCursor = it }
            actionResult.value = result
        }
    }

    /**
     * 流式发送用户消息：复用 enterSession 缓存的会话上下文，通过 SSE 接收增量回复。
     * 会话上下文未就绪时（未先调用 enterSession）直接抛错，不发起请求。
     */
    fun sendMessage(content: String) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        val flow = mRepo.api.sendMsgToAgent(
            JobAgentStreamBody(
                sessionId = currentSessionId,
                conversationId = currentConversationId,
                clientMessageId = UUID.randomUUID().toString(),
                content = content,
            )
        )
        sseHandler.start(flow)
    }

    /**
     * 取消当前流式生成。
     * @param requestId    服务端在流式 meta 事件中下发的 SSE 请求 ID；非空时会通知服务端取消，
     *                     为空则仅取消本地 SSE 收集（meta 事件解析落地后回填该值即可联通服务端取消）。
     * @param flag         true为主动打断 false为新消息打断
     */
    fun cancelStream(requestId: String? = null, flag: Boolean = true) {
        if (!requestId.isNullOrEmpty()) {
            request(
                apiBlock = {
                    mRepo.api.cancelStreamGenerate(
                        JobAgentCancelBody(
                            requestId = requestId,
                            conversationId = conversationId,
                            // 取消原因：USER_CANCEL 用户主动取消 / NEW_MESSAGE_INTERRUPTED 新消息打断
                            cancelReason = if (flag) "USER_CANCEL" else "NEW_MESSAGE_INTERRUPTED",
                        )
                    )
                }
            ) {
                // this.data：accepted / requestStatus，如需 UI 反馈可在此处理
            }
        }
        sseHandler.cancel()
        streaming.value = false
    }

    override fun onCleared() {
        sseHandler.cancel()
        super.onCleared()
    }
}