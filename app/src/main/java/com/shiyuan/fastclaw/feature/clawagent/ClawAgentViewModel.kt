package com.shiyuan.fastclaw.feature.clawagent

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.common.utils.LogUtils
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.PollingState
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchPolling
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.ActionSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.AgentMsgAction
import com.shiyuan.fastclaw.core.network.apis.request.AttributeArrayQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundCompanySearchQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundGenerateResultQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundPreCheckSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundRegenerateSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.ConversationQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.CurrentResumeQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.InterviewPassPackageResumeStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.JobAgentCancelBody
import com.shiyuan.fastclaw.core.network.apis.request.JobAgentStreamBody
import com.shiyuan.fastclaw.core.network.apis.request.JobStatusUpdateBody
import com.shiyuan.fastclaw.core.network.apis.request.MessagePageBody
import com.shiyuan.fastclaw.core.network.apis.request.MessagePullBody
import com.shiyuan.fastclaw.core.network.apis.request.PassPackageRegenerateBody
import com.shiyuan.fastclaw.core.network.apis.request.PassPackageStatusQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewForceCancelBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewReportProgressBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewReportRetryBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewStartBody
import com.shiyuan.fastclaw.core.network.apis.request.OnlineResumeAttributeQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeConfirmBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.SESSION_PAGE_SIZE
import com.shiyuan.fastclaw.core.network.apis.request.SessionEnterBody
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.response.ActionSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.ActivityStatusData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundCompanyInfo
import com.shiyuan.fastclaw.core.network.apis.response.ConversationQueryData
import com.shiyuan.fastclaw.core.network.apis.response.CurrentResumeData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentMessage
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.core.network.apis.response.MessagePageData
import com.shiyuan.fastclaw.core.network.apis.response.MessagePullData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewForceCancelData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeAttributeData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeConfirmData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.SessionEnterData
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.core.network.sse.RawSSEStreamHandler
import com.shiyuan.fastclaw.core.network.sse.processor.ClawAgentSseProcessor
import com.shiyuan.fastclaw.core.response
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * @createtime 2026年05月20日 10:27
 * @author TooWhiteT
 */

class ClawAgentViewModel : CommonRepositoryViewModel<AppRepository>() {

    /** 流是否正在输出中 */
    private val _streaming = MutableLiveData(false)
    val streaming: LiveData<Boolean> = _streaming

    /** 页面一次性效果；使用缓冲 Channel，避免 LiveData 重放和页面反向清空。 */
    private val _uiEffects = Channel<ClawAgentUiEffect>(Channel.BUFFERED)
    val uiEffects = _uiEffects.receiveAsFlow()

    /** 进入/恢复会话返回的数据（会话状态 + 历史消息） */
    private val _sessionData = MutableLiveData<SessionEnterData?>()
    val sessionData: LiveData<SessionEnterData?> = _sessionData

    /** 查询通信会话返回的数据（实例/对话/流式请求状态） */
    val conversationData = MutableLiveData<ConversationQueryData?>()

    /** 提交卡片/按钮动作返回的数据（含本次动作产生或更新的消息） */
    val actionResult = MutableLiveData<ActionSubmitData?>()

    /** 历史消息分页结果（向上翻页加载更早消息） */
    val historyMessages = MutableLiveData<MessagePageData?>()

    /** 增量消息拉取结果（拉取游标之后的新消息） */
    val pulledMessages = MutableLiveData<MessagePullData?>()

    /** 渲染用消息列表：已按端侧 itemType 映射并按序号排序，供 [ClawAgentMsgAdapter] submitList */
    private val _messageList = MutableLiveData<List<ClawAgentMessageInfo>>()
    val messageList: LiveData<List<ClawAgentMessageInfo>> = _messageList

    /**
     * 下拉刷新是否开启
     */
    private val _hasCanRefresh = MutableLiveData(false)
    val hasCanRefresh: LiveData<Boolean> = _hasCanRefresh

    /** 当前可确认简历摘要 */
    val currentResume = MutableLiveData<CurrentResumeData?>()

    /** 确认/否认当前简历的结果 */
    val resumeConfirmResult = MutableLiveData<ResumeConfirmData?>()

    /** 提交简历上传结果的返回 */
    val resumeUploadResult = MutableLiveData<ResumeUploadSubmitData?>()

    /** 查询属性数组候选值结果（工作经历/教育经历等表单数据） */
    val attributeArrayResult = MutableLiveData<AttributeArrayQueryData?>()

    /** 查询线上简历单个普通属性结果 */
    private val _jobStatusResult = MutableLiveData<OnlineResumeAttributeData?>()
    val jobStatusResult: LiveData<OnlineResumeAttributeData?> = _jobStatusResult

    /** 当前会话 ID，发送消息/提交动作时回传 */
    public var sessionId: String? = null

    /** 当前对话 ID */
    public var conversationId: String? = null

    /** 已持有的最新消息游标，用于增量恢复 */
    private var latestCursor: String? = null

    /** 历史分页：下一页向上翻页游标（服务端 nextBeforeCursor） */
    private var historyBeforeCursor: String? = null

    /** 历史分页：是否还有更早消息 */
    private var hasMoreHistory: Boolean = true

    /** 历史分页：是否正在加载，防并发下拉 */
    private var loadingHistory: Boolean = false

    /** 当前 SSE 请求 ID（meta 事件下发），用于取消/断线重连 */
     var currentRequestId: String? = null

    /** 当前 SSE 完成后是否需要拉取增量消息兜底 */
    private var shouldPullMessagesOnStreamComplete: Boolean = true

    /** 已接收的最后一条 SSE 事件序号，用于断线重连 lastSeq */
    private var lastSeq: Long? = null

    /** 记录是否初始化会话id, 用于判断是否拉取增量数据 */
    private val _hasInitSession = MutableLiveData(false)
    val hasInitSession: LiveData<Boolean> = _hasInitSession

    /** 公司背调报告生成结果轮询任务 */
    private var companyBackgroundGeneratePollingJob: Job? = null

    /** 模拟面试报告生成结果轮询任务，只保留最后一个轮询 */
    private var mockInterviewReportPollingJob: Job? = null

    /** 面试通关包报告生成状态轮询任务，只保留最后一个轮询 */
    private var passPackageReportPollingJob: Job? = null

    /** 简历恢复完成后的消息补拉轮询任务，只保留最后一个轮询。 */
    private var resumeMessagePollingJob: Job? = null

    /** 简历上传绑定流程中的临时 code，请求完成后清空，避免重复绑定。 */
    private val _resumeCode = MutableLiveData<String?>()
    val resumeCode: LiveData<String?> = _resumeCode

    /** 宝典页业务模块列表，按服务端返回顺序展示底部快捷入口。 */
    private val _jobAgentModuleList = MutableLiveData<List<JobAgentModuleData>>()
    val jobAgentModuleList: LiveData<List<JobAgentModuleData>> = _jobAgentModuleList

    /** 首页测薪资活动状态，用于游客欢迎卡后是否追加活动卡片。 */
    private val _activityStatus = MutableLiveData<ActivityStatusData?>()
    val activityStatus: LiveData<ActivityStatusData?> = _activityStatus

    private val messageStore by lazy {
        ClawAgentMessageStore(
            onMessagesChanged = { messages ->
                _messageList.value = messages
            },
            onUiEffect = ::emitUiEffect,
            onMockInterviewReportPolling = ::pollMockInterviewReportProgress,
            onPassPackageReportPolling = ::pollPassPackageReportProgress,
        )
    }

    private val sseProcessor by lazy {
        ClawAgentSseProcessor(
            onStart = {
                shouldPullMessagesOnStreamComplete = true
                _streaming.value = true
            },
            onMeta = { requestId, seq ->
                // 保存本次请求 ID 与序号，供取消/重连使用
                currentRequestId = requestId
                seq?.let { lastSeq = it }
            },
            onMessage = { msg ->
                // 记录游标
                latestCursor = msg.cursor
                shouldPullMessagesOnStreamComplete =
                    !messageStore.shouldSkipPullAfterStreamComplete(msg)
                // message/snapshot 是完整消息，按 messageId upsert
                messageStore.insertSseMessage(msg)
            },
            onPatch = { patch ->
                // patch 也携带 seq，记录后可用于断线重连 lastSeq。
                Log.d("sseProcessor_onPatch=","进入删除")
                patch.seq?.let { lastSeq = it }
                // 目前文档只要求处理 REMOVE：重新回答后移除旧答案气泡和旧下游问题卡。
                if (patch.patchType == PATCH_TYPE_REMOVE) {
                    messageStore.removeById(patch.messageId)
                }
            },
            onDone = { _, seq ->
                seq?.let { lastSeq = it }
                _streaming.value = false
            },
            onComplete = {
                messageStore.removeLastThinkMessage()
                // 拉取增量消息 前端兜底
                if (shouldPullMessagesOnStreamComplete) {
                    pullMessages()
                }
                shouldPullMessagesOnStreamComplete = true
                _streaming.value = false
            },
            onErrorCallback = { e ->
                shouldPullMessagesOnStreamComplete = true
                _streaming.value = false
                e?.let { emitUiEffect(ClawAgentUiEffect.ShowStreamError(it)) }
            }
        )
    }

    private val sseHandler by lazy {
        RawSSEStreamHandler(viewModelScope, sseProcessor)
    }

    private fun emitUiEffect(effect: ClawAgentUiEffect) {
        _uiEffects.trySend(effect)
    }

    private fun emitSessionNotReady() {
        emitUiEffect(
            ClawAgentUiEffect.ShowStreamError(
                IllegalStateException("会话未就绪，请先进入会话")
            )
        )
    }

    // region 会话相关

    /**
     * 进入或恢复会话：sessionId 为空表示首次进入，由服务端恢复最新会话
     */
    fun enterSession() {
        currentRequestId = UUID.randomUUID().toString()
        request(
            apiBlock = {
                mRepo.api.enterSession(
                    SessionEnterBody(
                        sessionId = sessionId,
                        conversationId = conversationId,
                        clientRequestId =currentRequestId,
                        latestCursor = latestCursor,
                        messageLimit = SESSION_PAGE_SIZE,
                    )
                )
            },
            showToast = true,
            onError = {
                _hasCanRefresh.value = true
            }
        ) {
            _hasInitSession.value = true
            val result = this.data ?: return@request
            // 记录会话上下文，供后续发送消息/增量恢复使用
            sessionId = result.sessionId
            conversationId = result.conversationId
            latestCursor = result.latestCursor
            _sessionData.value = result
            val hashMore = result.messages.orEmpty().size >= SESSION_PAGE_SIZE
            queryJobStatus(ATTRIBUTE_GROUP_JOB_STATUS)

            _hasCanRefresh.value = result.sessionId.isNullOrBlank() || result.conversationId.isNullOrBlank()
            // 恢复的最近消息为终态数据，过滤掉残留的思考态(think)中间态消息；并在 UPLOAD_RESUME 卡前补自我介绍卡
            val messages = result.messages?.filterNot { it.isThinkMessage() }
            ingestMessages(expandIntroduceBeforeUploadResume(messages, !hashMore))
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

    // endregion


    // region 消息相关

    /**
     * 拉取增量消息：服务端返回游标之后的新消息。
     * afterCursor 为空时默认用本地缓存的 latestCursor；成功后回写 latestCursor 供下次增量拉取。
     */
    fun pullMessages(afterCursor: String? = null, limit: Int = 20) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            return
        }
        request(
            apiBlock = {
                mRepo.api.pullMessages(
                    MessagePullBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
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
            //result.messages?.forEach { if (it.isToastCard()) { it.content?.showToast() } }
            // 如果原列表最后一条是 isThinkMessage，只保留最后这一条
            // 其它位置的 isThinkMessage 全部过滤
            val messages = result.messages?.filterIndexed { index, msg ->
                !msg.isThinkMessage() || index == result.messages.lastIndex
            }
            ingestMessages(messages)
            // 递归拉取
            if (result.hasMore == true) {
                pullMessages(latestCursor, limit)
            }
        }
    }

    /**
     * 简历恢复完成后立即补拉一次，并在限定时间内继续轮询。
     * 任务归 ViewModel 持有，页面销毁或账户重置时显式停止。
     */
    fun startResumeMessagePolling() {
        if (User.get().isVisitor || hasInitSession.value != true) return
        pullMessages(getLastMessageCursor())
        resumeMessagePollingJob?.cancel()
        resumeMessagePollingJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (
                isActive &&
                !User.get().isVisitor &&
                hasInitSession.value == true &&
                System.currentTimeMillis() - startTime < RESUME_MESSAGE_POLL_TIMEOUT_MS
            ) {
                delay(RESUME_MESSAGE_POLL_INTERVAL_MS)
                pullMessages(getLastMessageCursor())
            }
        }
    }

    fun stopResumeMessagePolling() {
        resumeMessagePollingJob?.cancel()
        resumeMessagePollingJob = null
    }

    /**
     * 加载更早历史消息（向上翻页 /message/page）。同时供「滚动接近顶部的无感知预取」和「下拉手动/重试」调用。
     * 首页用当前最早一条消息的 cursor 作为 beforeCursor，之后沿用服务端返回的 nextBeforeCursor；
     */
    fun loadOlderHistory() {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        // 未就绪
        if (currentSessionId.isNullOrEmpty() ||
            currentConversationId.isNullOrEmpty() ||
            // 加载中
            loadingHistory ||
            // 没有更早消息
            !hasMoreHistory
        ) {
            // 直接结束本次（下拉）
            return
        }
        loadingHistory = true
        val beforeCursor = historyBeforeCursor
            ?: messageStore.oldestCursor()
        // 用 invokeOnCompletion 兜底复位：业务错误走 request 的 onError，但 IO/解析异常被基础库 launch 的
        // CoroutineExceptionHandler 直接吞掉，不会回到这里的 onError，导致 loadingHistory 卡死
        request(
            apiBlock = {
                mRepo.api.queryMessagePage(
                    MessagePageBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        beforeCursor = beforeCursor,
                        pageSize = HISTORY_PAGE_SIZE,
                    )
                )
            },
            showToast = true,
        ) {
            val result = this.data
            historyMessages.value = result
            result?.nextBeforeCursor?.let { historyBeforeCursor = it }
            hasMoreHistory = result?.hasMore ?: false
            LogUtils.d("hasMore_${result?.hasMore}")
            // 更早消息 sequenceNo 更小，ingest 后按序排到列表顶部（DiffUtil 在 0 位插入，不跳动）
            // 历史分页为终态数据，过滤掉残留的思考态(think)中间态消息；并在 UPLOAD_RESUME 卡前补自我介绍卡
            val messages = result?.messages?.filterNot { it.isThinkMessage() }
            ingestMessages(expandIntroduceBeforeUploadResume(messages, result?.hasMore == false))
            // 已加载到最早一页时给一次轻提示
//            if (!hasMoreHistory) R.string.no_more_data_pull.getResString().showToast()
        }.invokeOnCompletion {
            loadingHistory = false
        }
    }

    /**
     * 发送消息时需要生成唯一clientMessageId，用于防重复提交
     */
    private fun generateClientMessageId(): String = "${UUID.randomUUID()}_${System.currentTimeMillis()}"

    /** 获取消息缓存中最后一条的 Cursor。 */
    fun getLastMessageCursor(): String? = messageStore.lastCursor()

    private fun ingestMessages(messages: List<JobAgentMessage>?) {
        messageStore.ingest(messages)
    }

    private fun expandIntroduceBeforeUploadResume(
        messages: List<JobAgentMessage>?,
        shouldInsertIntroduce: Boolean,
    ): List<JobAgentMessage> {
        return messageStore.expandIntroduceBeforeUploadResume(messages, shouldInsertIntroduce)
    }

    private fun JobAgentMessage.isThinkMessage(): Boolean = messageStore.isThinkMessage(this)

    /**
     * 流式发送用户消息：复用 enterSession 缓存的会话上下文，通过 SSE 接收增量回复。
     * 会话上下文未就绪时（未先调用 enterSession）直接抛错，不发起请求。
     */
    fun sendMessage(content: String) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            emitSessionNotReady()
            return
        }
        val clientMessageId = generateClientMessageId()
        // 乐观插入：先把用户消息加入列表立即反馈；服务端随后回显由 ingestMessages 插入服务端数据，
        // 但id用本地生成的，用服务端回显消息的话会因为id不同导致刷新两次体验不佳，(跟后端对齐过，这个消息的id只在sse流式中存在，拉历史数据影响不大)
        messageStore.appendLocalUserMessage(clientMessageId, content)
        val body = JobAgentStreamBody(
            sessionId = currentSessionId,
            conversationId = currentConversationId,
            clientMessageId = clientMessageId,
            content = content,
            interactionType = INTERACTION_TYPE_TEXT
        )
        val flow = mRepo.api.sendMsgToAgent(body)
        LogUtils.i("sseHandler=${GsonUtils.toJson(body)}")
        sseHandler.start(flow)
    }

    /**
     * 卡片交互 - sse流式
     */
    fun sendAction(content: String, actionData: AgentMsgAction) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            emitSessionNotReady()
            return
        }
        val clientMessageId = generateClientMessageId()
        val body = JobAgentStreamBody(
            sessionId = currentSessionId,
            conversationId = currentConversationId,
            clientMessageId = clientMessageId,
            content = content,
            interactionType = INTERACTION_TYPE_ACTION,
            action = actionData
        )
        val flow = mRepo.api.sendMsgToAgent(body)
        LogUtils.i("sseHandler=${GsonUtils.toJson(body)}")
        sseHandler.start(flow)
    }

    /**
     * 取消当前流式生成。
     * @param requestId    服务端在流式 meta 事件中下发的 SSE 请求 ID；非空时会通知服务端取消，
     *                     为空则仅取消本地 SSE 收集（meta 事件解析落地后回填该值即可联通服务端取消）。
     * @param flag         true为主动打断 false为新消息打断
     */
    fun cancelStream(requestId: String? = null, flag: Boolean = true) {
        // 缺省用 meta 事件回填的 currentRequestId
        val targetRequestId = requestId ?: currentRequestId
        if (!targetRequestId.isNullOrEmpty()) {
            request(
                apiBlock = {
                    mRepo.api.cancelStreamGenerate(
                        JobAgentCancelBody(
                            requestId = targetRequestId,
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
        _streaming.value = false
    }

    // endregion

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
            emitSessionNotReady()
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
            emitSessionNotReady()
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
            emitSessionNotReady()
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
            ingestMessages(result.messages)
        }
    }

    /**
     * 查询求职状态属性
     * @param attributeGroupCode 属性组编码
     */
    fun queryJobStatus(attributeGroupCode: String?) {
        request(
            apiBlock = {
                mRepo.api.queryOnlineResumeAttribute(
                    OnlineResumeAttributeQueryBody(attributeGroupCode = attributeGroupCode)
                )
            }
        ) {
            _jobStatusResult.value = data
        }
    }

    /**
     * 更新顶部求职状态
     */
    fun updateJobStatus(statusCode: String?) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty() || statusCode.isNullOrEmpty()) {
            emitSessionNotReady()
            return
        }
        request(
            apiBlock = {
                mRepo.api.updateJobStatus(
                    JobStatusUpdateBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        statusCode = statusCode,
                        clientRequestId = UUID.randomUUID().toString(),
                    )
                )
            },
            showToast = true,
        ) {
            //val result = this.data ?: return@request
            //sessionData.value = sessionData.value?.copy(jobSearchStatus = result)
            val jobStatusData = _jobStatusResult.value
            val selectedOption = jobStatusData?.options.orEmpty().firstOrNull { it.code == statusCode }
            _jobStatusResult.value = jobStatusData?.copy(displayValue = selectedOption?.name ?: jobStatusData.displayValue)
        }
    }

    /** 补问选项提交后先按旧逻辑乐观更新顶部求职状态文案。 */
    fun updateJobStatusDisplayValue(displayValue: String?) {
        _jobStatusResult.value = _jobStatusResult.value?.copy(displayValue = displayValue)
    }

    /** 退出登录时清除会话中的求职状态，保留会话对象其它字段。 */
    fun clearSessionJobSearchStatus() {
        _sessionData.value = _sessionData.value?.copy(jobSearchStatus = null)
    }

    /** 表单类补问共用同一个请求 ID；缺失时按旧逻辑即时生成。 */
    fun getOrCreateCurrentRequestId(): String {
        return currentRequestId?.takeIf { it.isNotEmpty() }
            ?: UUID.randomUUID().toString().also { currentRequestId = it }
    }

    fun queryCompanyBackgroundCompanySearch(keyword: String, onResult: (keyword: String, companies: List<CompanyBackgroundCompanyInfo>) -> Unit, ) {
        if (keyword.isEmpty()) {
            onResult(keyword, emptyList())
            return
        }
        request(
            apiBlock = {
                mRepo.api.queryCompanyBackgroundCompanySearch(
                    CompanyBackgroundCompanySearchQueryBody(keyword = keyword)
                )
            },
            showToast = false,
            onError = {
                onResult(keyword, emptyList())
            },
        ) {
            onResult(keyword, data?.items.orEmpty())
        }
    }

    /**
     * 公司背调 - 提交报告生成前置校验。
     * 后端返回 preCheckId 后立即进入前置校验轮询，失败按文档 errorMessage/errorCode 提示。
     */
    fun submitCompanyBackgroundPreCheck(companyId: Long?) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            emitSessionNotReady()
            return
        }
        val clientRequestId = "company-bg-precheck-${System.currentTimeMillis()}-${UUID.randomUUID()}"
        requestWithLoading(
            apiBlock = {
                mRepo.api.submitCompanyBackgroundPreCheck(
                    CompanyBackgroundPreCheckSubmitBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        clientRequestId = clientRequestId,
                        companyId = companyId,
                    )
                )
            }
        ) {
            val reportId = data?.reportId
            if (reportId.isNullOrBlank()) {
                emitUiEffect(ClawAgentUiEffect.HandleCompanyBackgroundPreCheck(data))
                return@requestWithLoading
            }
            // 权益可用时后端会写入“正在识别公司名称”，提交成功后先拉取一次增量消息展示处理中状态。
            pullMessages(getLastMessageCursor())
            pollCompanyBackgroundGenerateResult(reportId)
        }
    }

    /**
     * 轮询公司背调报告生成结果。
     * 该接口只返回 finished，终态成功/失败卡仍通过消息拉取展示。
     */
    private fun pollCompanyBackgroundGenerateResult(reportId: String) {
        companyBackgroundGeneratePollingJob?.cancel()
        companyBackgroundGeneratePollingJob = launchPolling(
            intervalMs = 5000L,
            retryOnNetworkError = true,
            apiBlock = {
                mRepo.api.queryCompanyBackgroundGenerateResult(
                    CompanyBackgroundGenerateResultQueryBody(reportId = reportId)
                )
            },
            checkState = { response ->
                if (response?.data?.finished == true) PollingState.SUCCESS else PollingState.CONTINUE
            },
            onSuccess = {
                // 报告到终态后再拉取同 responseGroupId 下的成功或失败卡。
                pullMessages(getLastMessageCursor())
            },
        )
    }

    /**
     * 公司背调 - 重新生成失败报告。
     * 接口成功后使用新 reportId 继续轮询，生成中/终态卡片仍通过 message/pull 展示。
     */
    fun retryGenerateCompanyBackgroundReport(failedReportId: String?) {
        companyBackgroundGeneratePollingJob?.cancel()
        requestWithLoading(
            apiBlock = {
                mRepo.api.regenerateCompanyBackgroundReport(
                    CompanyBackgroundRegenerateSubmitBody(
                        failedReportId = failedReportId,
                        clientRequestId = "company-bg-regenerate-${System.currentTimeMillis()}-${UUID.randomUUID()}",
                    )
                )
            }
        ) {
            val reportId = data?.reportId
            if (reportId.isNullOrBlank()) {
                emitUiEffect(ClawAgentUiEffect.HandleCompanyBackgroundPreCheck(data))
                return@requestWithLoading
            }
            pullMessages(getLastMessageCursor())
            pollCompanyBackgroundGenerateResult(reportId)
        }
    }

    /**
     * 模拟面试报告 - 查询报告生成进度。
     * 对应文档 9.1：GENERATING 继续轮询，SUCCESS/FAILED_* 到终态后拉取增量消息刷新报告卡。
     */
    private fun pollMockInterviewReportProgress(mockReportId: String?, interviewId: String?) {
        if (mockReportId.isNullOrBlank() || interviewId.isNullOrBlank()) return
        mockInterviewReportPollingJob?.cancel()
        mockInterviewReportPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true,
            apiBlock = {
                mRepo.api.queryMockInterviewReportProgress(
                    MockInterviewReportProgressBody(
                        mockReportId = mockReportId,
                        interviewId = interviewId,
                    )
                )
            },
            checkState = { response ->
                when (response?.data?.reportStatus) {
                    REPORT_STATUS_SUCCESS -> PollingState.SUCCESS
                    REPORT_STATUS_FAILED,
                    REPORT_STATUS_FAILED_RETRYABLE,
                    REPORT_STATUS_FAILED_FINAL -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                mockInterviewReportPollingJob = null
                pullMessages(getLastMessageCursor())
            },
            onFail = { response, _ ->
                mockInterviewReportPollingJob = null
                pullMessages(getLastMessageCursor())
            },
        )
    }

    /**
     * 模拟面试报告 - 重新生成失败报告。
     * 对应文档 9.2：重试成功后不重新进入面试间，只重新启动最后一个报告进度轮询。
     */
    fun retryGenerateMockInterviewReport(mockReportId: String?, interviewId: String?) {
        if (mockReportId.isNullOrBlank() || interviewId.isNullOrBlank()) return
        mockInterviewReportPollingJob?.cancel()
        mockInterviewReportPollingJob = null
        requestWithLoading(
            apiBlock = {
                mRepo.api.retryMockInterviewReport(
                    MockInterviewReportRetryBody(
                        mockReportId = mockReportId,
                        interviewId = interviewId,
                        reportRetryId = "mock-report-retry-${System.currentTimeMillis()}-${UUID.randomUUID()}",
                    )
                )
            }
        ) {
            pollMockInterviewReportProgress(mockReportId, interviewId)
            pullMessages(getLastMessageCursor())
        }
    }

    /**
     * 面试通关包报告 - 查询生成状态。
     * 0/1 继续轮询，2 成功，3/4 失败；终态后拉取增量消息刷新卡片。
     */
    private fun pollPassPackageReportProgress(packageId: String?) {
        if (packageId.isNullOrBlank()) return
        passPackageReportPollingJob?.cancel()
        passPackageReportPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true,
            apiBlock = {
                mRepo.api.queryPassPackageProgress(
                    PassPackageStatusQueryBody(packageId = packageId)
                )
            },
            checkState = { response ->
                when (response?.data?.packageStatus) {
                    PASS_PACKAGE_STATUS_SUCCESS -> PollingState.SUCCESS
                    PASS_PACKAGE_STATUS_FAILED,
                    PASS_PACKAGE_STATUS_BLOCKED -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = {
                passPackageReportPollingJob = null
                pullMessages(getLastMessageCursor())
            },
            onFail = { _, _ ->
                passPackageReportPollingJob = null
                pullMessages(getLastMessageCursor())
            },
        )
    }

    /**
     * 面试通关包报告 - 重新生成。
     * 重新生成成功后复用通关包进度轮询，终态由增量消息刷新卡片。
     */
    fun retryGeneratePassPackageReport(packageId: String?) {
        if (packageId.isNullOrBlank()) return
        passPackageReportPollingJob?.cancel()
        passPackageReportPollingJob = null
        requestWithLoading(
            apiBlock = {
                mRepo.api.regeneratePassPackageReport(
                    PassPackageRegenerateBody(packageId = packageId)
                )
            }
        ) {
            val newPackageId = data?.packageId ?: packageId
            pullMessages(getLastMessageCursor())
            pollPassPackageReportProgress(newPackageId)
        }
    }

    /**
     * 查询属性数组候选值（工作经历、教育经历等表单数据）。
     * @param questionQueueId 问题队列 ID
     * @param attributeGroupCode 属性组编码
     * @param onResult 查询结果回调
     */
    fun queryAttributeArray(questionQueueId: String?, attributeGroupCode: String?, onResult: (AttributeArrayQueryData?) -> Unit) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            emitSessionNotReady()
            return
        }
        requestWithLoading(
            apiBlock = {
                mRepo.api.queryAttributeArray(
                    AttributeArrayQueryBody(
                        sessionId = currentSessionId,
                        conversationId = currentConversationId,
                        questionQueueId = questionQueueId,
                        attributeGroupCode = attributeGroupCode,
                    )
                )
            }
        ) {
            this.data?.clientRequestId = currentRequestId
            onResult(this.data)
        }
    }

    fun startInterview(entryMessageId: String?, entrySource: String?, resumeId: String?) {
        request(
            apiBlock = {
                mRepo.api.startMockInterviewSession(
                    MockInterviewStartBody(
                        entryMessageId = entryMessageId,
                        entrySource = entrySource,
                        resumeId = resumeId,
                        chargeRequestId = UUID.randomUUID().toString(),
                    )
                )
            },
            showToast = true,
            {
               it?.message?.showToast()
            }
        ) {
            emitUiEffect(ClawAgentUiEffect.HandleInterviewStart(data))
        }
    }

    /**
     * 强制结束当前模拟面试。
     */
    fun forceFinish(interviewId: String,onSuccess: (data: MockInterviewForceCancelData?) -> Unit) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.forceCancelMockInterview(
                    MockInterviewForceCancelBody(
                        interviewId = interviewId,
                        forceCancelRequestId = UUID.randomUUID().toString(),
                        cancelReason = FORCE_CANCEL_REASON_USER,
                    )
                )
            },
            showToastError = true,
        ) {
            onSuccess(this.data)
        }
    }

    fun queryInterviewPassPackageResumeStatus() {
        requestWithLoading(
            apiBlock = {
                mRepo.api.queryInterviewPassPackageResumeStatus(
                    InterviewPassPackageResumeStatusBody()
                )
            },
            showLoading = true,
            showToastError = true,
        ) {
            emitUiEffect(ClawAgentUiEffect.OpenInterviewPassPackage(this.data))
        }
    }

    /**
     * 查询是否有未结束的面试
     */
    fun queryUnfinishedInterview() {
        request(
            apiBlock = { mRepo.api.queryMockInterviewUnfinished() },
            showToast = false,
        ) {
            data?.let {
                emitUiEffect(ClawAgentUiEffect.ShowInterviewRecovery(it))
            }
        }
    }

    /**
     * 根据授权码查询简历地址
     */
    fun queryCodeResumeBinding(code: String, onSuccess: (data: ResumeData?) -> Unit) {
        launch(block = {
            response(
                mRepo.api.queryCodeResumeBinding(mapOf("code" to code)),
                result = {
                    //data 可能为null
                    data?.let {
                        onSuccess(data)
                    }
                }, error = {

                })
        }, onComplete = {
            _resumeCode.value = null
        })
    }

    fun setResumeCode(code: String?) {
        _resumeCode.value = code
    }

    /** 获取宝典页业务模块列表，用于动态渲染首页底部快捷入口。 */
    fun getAgentModuleList() {
        request(
            apiBlock = { mRepo.api.queryJobAgentModuleList() },
            showToast = false,
        ) {
            _jobAgentModuleList.postValue(this.data.orEmpty())
        }
    }

    /** 查询首页测薪资活动状态；异常时按不展示处理。 */
    fun queryActivityStatus() {
        request(
            apiBlock = { mRepo.api.queryActivityStatus() },
            showToast = false,
        ) {
            _activityStatus.postValue(data)
        }
    }


    /**
     * 退出登录 用来重置
     */
    fun reset() {
        // sse 重置
        sseHandler.cancel()
        // 会话列表重置
        sessionId = null
        conversationId = null
        // 消息列表重置
        latestCursor = null
        messageStore.clear()
        _messageList.value = emptyList()
        // 下拉判定条件重置
        historyBeforeCursor = null
        hasMoreHistory = true
        loadingHistory = false
        mockInterviewReportPollingJob?.cancel()
        mockInterviewReportPollingJob = null
        passPackageReportPollingJob?.cancel()
        passPackageReportPollingJob = null
        stopResumeMessagePolling()
    }

    fun showLocalMessageCards(list: List<ClawAgentMessageInfo>) {
        messageStore.showLocalMessages(list)
    }

    override fun onDestroy() {
        sseHandler.cancel()
        stopResumeMessagePolling()
        companyBackgroundGeneratePollingJob?.cancel()
        mockInterviewReportPollingJob?.cancel()
        mockInterviewReportPollingJob = null
        passPackageReportPollingJob?.cancel()
        passPackageReportPollingJob = null
        super.onDestroy()
    }

    companion object {
        private const val HISTORY_PAGE_SIZE = 20
        private const val RESUME_MESSAGE_POLL_INTERVAL_MS = 10000L
        private const val RESUME_MESSAGE_POLL_TIMEOUT_MS = 60000L
        private const val INTERACTION_TYPE_TEXT = "TEXT"
        private const val INTERACTION_TYPE_ACTION = "ACTION"

        const val SENDER_USER = "USER"
        const val SENDER_AGENT = "AGENT"
        const val QUICK_ENTRY = "QUICK_ENTRY"
        const val ATTRIBUTE_GROUP_JOB_STATUS = "jobStatus"
        const val ERROR_CODE_NOT_COMPANY = "NOT_COMPANY"
        const val ERROR_CODE_POINTS_INSUFFICIENT = "POINTS_INSUFFICIENT"
        const val ERROR_CODE_MODEL_FAILED = "MODEL_FAILED"

        private const val REPORT_STATUS_SUCCESS = "SUCCESS"
        private const val REPORT_STATUS_FAILED = "FAILED"
        private const val REPORT_STATUS_FAILED_RETRYABLE = "FAILED_RETRYABLE"
        private const val REPORT_STATUS_FAILED_FINAL = "FAILED_FINAL"
        private const val PASS_PACKAGE_STATUS_SUCCESS = 2
        private const val PASS_PACKAGE_STATUS_FAILED = 3
        private const val PASS_PACKAGE_STATUS_BLOCKED = 4
        private const val PATCH_TYPE_REMOVE = "REMOVE"
        private const val FORCE_CANCEL_REASON_USER = "USER_FORCE_CANCEL"
    }
}
