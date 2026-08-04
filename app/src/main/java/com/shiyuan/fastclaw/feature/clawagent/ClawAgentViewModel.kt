package com.shiyuan.fastclaw.feature.clawagent

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.common.utils.LogUtils
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
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
import com.shiyuan.fastclaw.core.network.apis.request.SessionEnterBody
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.response.ActionSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeConfirmPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundCompanyInfo
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundPreCheckResultData
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.ConversationQueryData
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CurrentResumeData
import com.shiyuan.fastclaw.core.network.apis.response.HistoryResumeConfirmPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.InterviewPassPackageResumeStatusData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentMessage
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
import com.shiyuan.fastclaw.core.network.apis.response.MessagePageData
import com.shiyuan.fastclaw.core.network.apis.response.MessagePullData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewEntryPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewForceCancelData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewStartData
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewUnfinishedData
import com.shiyuan.fastclaw.core.network.apis.response.OnlineResumeAttributeData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeConfirmData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.SessionEnterData
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.core.network.sse.RawSSEStreamHandler
import com.shiyuan.fastclaw.core.network.sse.processor.ClawAgentSseProcessor
import com.shiyuan.fastclaw.core.response
import kotlinx.coroutines.Job
import java.util.UUID

/**
 * @createtime 2026年05月20日 10:27
 * @author TooWhiteT
 */

class ClawAgentViewModel : CommonRepositoryViewModel<AppRepository>() {

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

    /** 渲染用消息列表：已按端侧 itemType 映射并按序号排序，供 [ClawAgentMsgAdapter] submitList */
    val messageList = MutableLiveData<List<ClawAgentMessageInfo>>()

    /**
     * 下拉刷新是否开启
     */
    val hasCanRefresh = MutableLiveData<Boolean>(false)

    /** 当前可确认简历摘要 */
    val currentResume = MutableLiveData<CurrentResumeData?>()

    /** 确认/否认当前简历的结果 */
    val resumeConfirmResult = MutableLiveData<ResumeConfirmData?>()

    /** 提交简历上传结果的返回 */
    val resumeUploadResult = MutableLiveData<ResumeUploadSubmitData?>()

    /** 收到 ATTRIBUTE_CONFIRM 卡时触发记忆确认弹窗（一次性事件，携带消息供弹窗解析展示） */
    val attributeConfirmPopup = MutableLiveData<ClawAgentMessageInfo?>()

    /** 开启消息通知弹窗 */
    val understandingSummaryDialog = MutableLiveData<ClawAgentMessageInfo?>()

    /** 查询属性数组候选值结果（工作经历/教育经历等表单数据） */
    val attributeArrayResult = MutableLiveData<AttributeArrayQueryData?>()

    /** 查询线上简历单个普通属性结果 */
    val jobStatusResult = MutableLiveData<OnlineResumeAttributeData?>()

    /** 模拟面试启动结果，用于页面进入面试房间或处理启动失败状态。 */
    val interviewStartResult = MutableLiveData<MockInterviewStartData?>()

    /** 查询可恢复未结束模拟面试结果 */
    val interviewUnfinishedResult = MutableLiveData<MockInterviewUnfinishedData?>()

    /** 公司背调报告生成前置校验轮询结果 */
    val companyBackgroundResult = MutableLiveData<CompanyBackgroundPreCheckResultData?>()

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

    /** 按消息 ID 去重保存的原始消息，便于增量/历史合并后统一映射并按 sequenceNo 排序 */
    private val messageStore = LinkedHashMap<String, JobAgentMessage>()

    /** 当前 SSE 请求 ID（meta 事件下发），用于取消/断线重连 */
     var currentRequestId: String? = null

    /** 已接收的最后一条 SSE 事件序号，用于断线重连 lastSeq */
    private var lastSeq: Long? = null

    /** 本地乐观插入的用户消息 ID，待服务端回显后把服务端字段填充进同一占位（保留 id） */
    private val optimisticUserMessageIds = LinkedHashSet<String>()

    /** 服务端用户消息真实 key -> 本地乐观占位 key，避免 SSE 合并后 pull/page 再按真实 messageId 插入重复项 */
    private val serverUserMessageKeyToLocalKey = HashMap<String, String>()

    /** 记录是否初始化会话id, 用于判断是否拉取增量数据 */
    val hasInitSession = MutableLiveData<Boolean>(false)

    /** 公司背调报告生成结果轮询任务 */
    private var companyBackgroundGeneratePollingJob: Job? = null

    /** 模拟面试报告生成结果轮询任务，只保留最后一个轮询 */
    private var mockInterviewReportPollingJob: Job? = null

    /** 面试通关包报告生成状态轮询任务，只保留最后一个轮询 */
    private var passPackageReportPollingJob: Job? = null

    /** 简历上传绑定流程中的临时 code，请求完成后清空，避免重复绑定。 */
    val resumeCode = MutableLiveData<String?>()

    /** 面试通关包开始前的简历状态，用于判断是否已有可用简历或需要先上传简历。 */
    val interviewPassPackageResumeStatus = MutableLiveData<InterviewPassPackageResumeStatusData?>()

    /** 宝典页业务模块列表，按服务端返回顺序展示底部快捷入口。 */
    val jobAgentModuleList = MutableLiveData<List<JobAgentModuleData>>()

    private val sseProcessor by lazy {
        ClawAgentSseProcessor(
            onStart = {
                streaming.value = true
                streamError.value = null
            },
            onMeta = { requestId, seq ->
                // 保存本次请求 ID 与序号，供取消/重连使用
                currentRequestId = requestId
                seq?.let { lastSeq = it }
            },
            onMessage = { msg ->
                // 记录游标
                latestCursor = msg.cursor
                // message/snapshot 是完整消息，按 messageId upsert
                sseInsertMessages(msg)
            },
            onPatch = { patch ->
                // patch 也携带 seq，记录后可用于断线重连 lastSeq。
                Log.d("sseProcessor_onPatch=","进入删除")
                patch.seq?.let { lastSeq = it }
                // 目前文档只要求处理 REMOVE：重新回答后移除旧答案气泡和旧下游问题卡。
                if (patch.patchType == PATCH_TYPE_REMOVE) {
                    removeMessageById(patch.messageId)
                }
            },
            onDone = { _, seq ->
                seq?.let { lastSeq = it }
                streaming.value = false
            },
            onComplete = {
                checkLastMessages()
                // 拉取增量消息 前端兜底
                pullMessages()
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
                    )
                )
            },
            showToast = true,
            onError = {
                hasCanRefresh.value = true
            }
        ) {
            hasInitSession.value = true
            val result = this.data ?: return@request
            // 记录会话上下文，供后续发送消息/增量恢复使用
            sessionId = result.sessionId
            conversationId = result.conversationId
            latestCursor = result.latestCursor
            sessionData.value = result
            queryJobStatus(ATTRIBUTE_GROUP_JOB_STATUS)

            hasCanRefresh.value = result.sessionId.isNullOrBlank() || result.conversationId.isNullOrBlank()
            // 恢复的最近消息为终态数据，过滤掉残留的思考态(think)中间态消息；并在 UPLOAD_RESUME 卡前补自我介绍卡
            ingestMessages(expandIntroduceBeforeUploadResume(result.messages?.filterNot { it.isThinkMessage() }))
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
            //streamError.value = IllegalStateException("会话未就绪，请先进入会话")
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
            ?: messageStore.values.minByOrNull { it.sequenceNo ?: Long.MAX_VALUE }?.cursor
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
            // 更早消息 sequenceNo 更小，ingest 后按序排到列表顶部（DiffUtil 在 0 位插入，不跳动）
            // 历史分页为终态数据，过滤掉残留的思考态(think)中间态消息；并在 UPLOAD_RESUME 卡前补自我介绍卡
            ingestMessages(expandIntroduceBeforeUploadResume(result?.messages?.filterNot { it.isThinkMessage() }))
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

    /**
     * 获取消息缓存中最后一条的Cursor
     */
    fun getLastMessageCursor(): String? {
        val lastEntry = messageStore.entries.lastOrNull()
        val lastMsg = lastEntry?.value
        return lastMsg?.cursor
    }

    /**
     * stream 结束 or 断开时如果最近消息是 Think类型，则移除
     */
    private fun checkLastMessages() {
        val lastEntry = messageStore.entries.lastOrNull()
        val lastMsg = lastEntry?.value
        if (lastMsg != null && resolveItemType(lastMsg) == ClawAgentMsgAdapter.TYPE_AGENT_THINK) {
            messageStore.remove(lastEntry.key)
        }
    }

    /**
     * 乐观插入本地用户消息：分配比当前最大序号更大的 sequenceNo 以排到列表末尾，messageId 取 clientMessageId。
     * 服务端随后会通过 SSE message(USER) 回显（不同 messageId），由 [ingestMessages] 按内容把服务端字段填充进本占位（保留占位 id，不刷新）。
     */
    private fun appendLocalUserMessage(clientMessageId: String, content: String) {
        val nextSeq = (messageStore.values.mapNotNull { it.sequenceNo }.maxOrNull() ?: 0L) + 1
        optimisticUserMessageIds.add(clientMessageId)
        ingestMessages(
            listOf(
                JobAgentMessage(
                    messageId = clientMessageId,
                    sequenceNo = nextSeq,
                    senderType = SENDER_USER,
                    messageType = MSG_TEXT,
                    content = content,
                )
            )
        )
    }

    /**
     * SSE 流-单条消息插入：按文档 5.3 displayPolicy 在插入前对「链表上一条思考态消息」做即时收口。
     * SSE 是顺序流，结果消息通常紧跟在 think 占位之后，故先取 [messageStore] 最后一条（上一条）：
     * 若它是 think 中间态、且与新消息非同一条（messageId 不同，排除 think→RESULT 的同条更新），
     * 再按其 displayPolicy 决定去留——
     * REPLACE_ON_TERMINAL（被结果替换）/ DISMISS_ON_TERMINAL（结果到达后收起）→ 移除该 think；
     * APPEND（追加）/ KEEP（保留）/ 其它 → 原样保留。最后统一交 [ingestMessages] upsert 并刷新。
     */
    private fun sseInsertMessages(newMsg: JobAgentMessage?) {
        newMsg ?: return
        val lastEntry = messageStore.entries.lastOrNull()
        val lastMsg = lastEntry?.value
        if (lastMsg != null &&
            lastMsg.messageId != newMsg.messageId &&
            resolveItemType(lastMsg) == ClawAgentMsgAdapter.TYPE_AGENT_THINK
        ) {
            when (lastMsg.displayPolicy) {
                // 终态替换：结果消息取代 think 占位
                POLICY_REPLACE_ON_TERMINAL -> messageStore.remove(lastEntry.key)
                // 终态移除：think / 轻提示在结果到达后收起
                POLICY_DISMISS_ON_TERMINAL -> messageStore.remove(lastEntry.key)
                // APPEND / KEEP / 其它：保留上一条 think
                else -> Unit
            }
        }
        // ATTRIBUTE_CONFIRM 卡：触发记忆确认弹窗（不进列表，由 ingestMessages 统一过滤）
        if (newMsg.isAttributeConfirm()) {
            val payload = newMsg.payloadJson?.let {
                runCatching { GsonUtils.fromJson(newMsg.payloadJson, AttributeConfirmPayloadBean::class.java) }.getOrNull()
            }
            payload?.interactionState?.let { state ->
                // 未消费，消费过的卡片不再弹
                if (state.disabled == false) {
                    attributeConfirmPopup.value = newMsg.toMessageInfo()
                }
            }
        }
        if (newMsg.isNotificationSwitch()) {
            understandingSummaryDialog.value = newMsg.toMessageInfo()
        }
        ingestMessages(listOf(newMsg), isReal = true)
    }

    /**
     * 测试记忆卡片弹窗
     */
//    fun testPopupMsg() {
//        attributeConfirmPopup.value = ClawAgentMessageInfo(
//            id = generateClientMessageId(),
//            messageType = 1,
//            sendObj = SENDER_AGENT,
//            receiveObj = SENDER_USER,
//            content = "记忆卡片测试",
//            metaData = GsonUtils.toJson(mapOf<String, String>("memoryText" to "记忆卡片 - 问题"))
//        )
//    }

    /**
     * 合并并刷新渲染列表：按消息 ID 去重（同 ID 覆盖以实现更新/替换），
     * 再按 sequenceNo 升序映射为端侧 [ClawAgentMessageInfo] 后回推 [messageList]。
     */
    private fun ingestMessages(messages: List<JobAgentMessage>?, isReal: Boolean = false) {
        if (messages.isNullOrEmpty()) return
        messages.forEach { newMsg ->
            // ATTRIBUTE_CONFIRM 走记忆确认弹窗，不进对话列表（enterSession / 历史分页 / 增量拉取统一过滤）
            if (newMsg.isAttributeConfirm()) return@forEach
            if (newMsg.isNotificationSwitch()) return@forEach
            // HISTORY_RESUME_CONFIRM 已操作过（interactionState.disabled=true）的不展示：
            // 覆盖 SSE 实时操作后服务端回写禁用态 与 enterSession / 历史分页里已操作过的残留两种场景；
            // 同时把 messageStore 里已渲染的同 id 旧卡移除，避免之前的可点击态滞留在列表
            if (newMsg.isHistoryResumeConfirmCard()) {
                val disabled = newMsg.payloadJson?.let {
                    runCatching {
                        GsonUtils.fromJson(it, HistoryResumeConfirmPayloadBean::class.java)?.interactionState?.disabled
                    }.getOrNull()
                }
                if (disabled == true) {
                    val key = newMsg.messageId ?: newMsg.cursor ?: newMsg.sequenceNo?.toString()
                    key?.let { messageStore.remove(it) }
                    return@forEach
                }
            }
            // 待上传和上传失败 在历史消息中已操作过的不展示，在实时sse的消息中要进行更新展示
            if (newMsg.isUploadResumeCard()) {
                val interactionState = newMsg.payloadJson?.let {
                    runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java)?.interactionState }.getOrNull()
                }
                if (interactionState?.disabled == true) {
                    // 实时sse需要删除操作过的消息，历史和拉取
                    if (isReal) {
                        val key = newMsg.messageId ?: newMsg.cursor ?: newMsg.sequenceNo?.toString()
                        key?.let { messageStore.remove(it) }
                    }
                    return@forEach
                }
            }
            val rawKey = newMsg.messageId ?: newMsg.cursor ?: newMsg.sequenceNo?.toString() ?: return@forEach
            // 自我介绍卡是端侧补充卡，折叠屏重建或会话恢复可能重复补充，这里只保留最新一张
            if (newMsg.isIntroduceYourselfCard()) {
                messageStore.filterValues { it.isIntroduceYourselfCard() }
                    .keys
                    .filter { it != rawKey }
                    .forEach { messageStore.remove(it) }
            }
            // 简历待上传/失败/确认流程卡类型合并只展示最新一张
            if (newMsg.isLatestOnlyResumeCard()) {
                val msgSeq = newMsg.sequenceNo ?: Long.MIN_VALUE
                // 筛选出所有关于“简历流程卡”类型的消息列表
                val resumeCards = messageStore.filterValues { it.isLatestOnlyResumeCard() }
                // 历史分页可能返回旧卡，不能覆盖当前更新的简历流程状态
                // 有没有任意一个元素满足条件
                if (resumeCards.any { it.key != rawKey && msgSeq < (it.value.sequenceNo ?: Long.MIN_VALUE) }) {
                    return@forEach
                }
                // 当前卡将要入库，先移除其它简历流程卡，保证列表只展示最新一张
                resumeCards.keys
                    .filter { it != rawKey }
                    .forEach { messageStore.remove(it) }
            }
            var key = rawKey
            if (newMsg.senderType == SENDER_USER) {
                // 已合并过的服务端用户消息，继续复用本地占位 key，避免 pull/page 重复插入
                key = serverUserMessageKeyToLocalKey[rawKey] ?: rawKey
                if (key != rawKey) {
                    messageStore.remove(rawKey)
                    messageStore[key] = newMsg.copy(messageId = key)
                    newMsg.rememberServerKeys(key)
                    return@forEach
                }
            }
            // 服务端回显的用户消息：把服务端字段填充进本地乐观占位，但保留占位的 messageId
            // 以维持 item 身份（id 不变 → 不触发 remove+insert，仅静默 change），避免气泡刷两次
            if (newMsg.senderType == SENDER_USER && key !in optimisticUserMessageIds) {
                val localKey = optimisticUserMessageIds.firstOrNull { messageStore[it]?.content == newMsg.content }
                if (localKey != null) {
                    messageStore.remove(rawKey)
                    messageStore[localKey] = newMsg.copy(messageId = localKey)
                    optimisticUserMessageIds.remove(localKey)
                    // 首次把服务端回显合并进本地占位时，记录真实 key 到本地 key 的映射
                    newMsg.rememberServerKeys(localKey)
                    // 已填充进占位，不再以服务端 messageId 另存，避免重复
                    return@forEach
                }
            }
            // 同一响应组内，REPLACE_ON_TERMINAL 的中间态消息由后续终态/结果消息替换。
            // 移除需要替换的消息
            newMsg.responseGroupId?.takeIf { it.isNotBlank() }?.let { groupId ->
                    messageStore.filterValues { it.responseGroupId == groupId && (it.displayPolicy == POLICY_REPLACE_ON_TERMINAL
                            || it.displayPolicy == POLICY_DISMISS_ON_TERMINAL) }
                        .keys
                        .filter { it != key }
                        .forEach { messageStore.remove(it) }
                }
            // 最后如果是Toast消息，将被丢弃不插入处理
            if (!newMsg.isToastCard()) {
                // 插入或替换消息，显示位置最终会按[sequenceNo]排序
                messageStore[key] = newMsg
            }
        }
        startPollingIfVisible()
        refreshMessageList()
    }

    /** 对本批消息中最终仍展示在 messageStore 里的生成中报告卡启动轮询。 */
    private fun startPollingIfVisible() {
        messageStore.values.forEach { msg ->
            // 是否模拟面试报告生成中，启动面试报告轮询接口
            if (msg.isMockInterviewReportCard()) {
                val payload = msg.payloadJson?.let {
                    runCatching { GsonUtils.fromJson(it, MockInterviewReportPayloadBean::class.java) }.getOrNull()
                }
                if (payload?.reportStatus == REPORT_STATUS_GENERATING) {
                    pollMockInterviewReportProgress(payload.mockReportId, payload.interviewId)
                }
            }
            // 是否面试通关包报告生成中，启动通关包报告轮询接口
            if (msg.isPassPackageReportCard()) {
                val payload = msg.payloadJson?.let {
                    runCatching { GsonUtils.fromJson(it, PassPackageReportPayloadBean::class.java) }.getOrNull()
                }
                if (payload?.reportStatus == REPORT_STATUS_GENERATING) {
                    pollPassPackageReportProgress(payload.packageId)
                }
            }
        }
    }

    /**
     * 处理 SSE patch REMOVE：按服务端 messageId 从当前内存列表移除消息。
     * 用户消息可能已合并到本地乐观占位 key，这里同时通过 serverUserMessageKeyToLocalKey 找到本地 key 删除。
     */
    private fun removeMessageById(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        // 用户气泡可能已被服务端回显合并到本地乐观占位，patch 给的是服务端 messageId，
        // 所以先查映射表，找到真正展示在 messageStore 里的本地 key。
        val localKey = serverUserMessageKeyToLocalKey[messageId]
        val keysToRemove = messageStore
            .filter { (key, msg) ->
                // messageStore 的 key 通常是 messageId；历史/兜底场景也可能使用 cursor 或 sequenceNo。
                key == messageId ||
                    key == localKey ||
                    msg.messageId == messageId ||
                    msg.cursor == messageId ||
                    msg.sequenceNo?.toString() == messageId
            }
            .keys
            .toMutableSet()
        // 如果命中了本地乐观占位 key，即使当前 filter 没扫到，也加入待删除集合。
        localKey?.let(keysToRemove::add)
        if (keysToRemove.isEmpty()) return
        keysToRemove.forEach { key ->
            messageStore.remove(key)
            optimisticUserMessageIds.remove(key)
        }
        // 清理服务端 key -> 本地 key 映射，避免后续 pull/page 再把已删除消息映射回本地占位。
        serverUserMessageKeyToLocalKey.entries.removeAll { (serverKey, mappedLocalKey) ->
            serverKey == messageId || serverKey in keysToRemove || mappedLocalKey in keysToRemove
        }
        Log.d("sseProcessor_onPatch=",messageId)
        refreshMessageList()
    }

    /** 按 sequenceNo 升序映射并回推渲染列表。 */
    private fun refreshMessageList() {
        val rawMessages = messageStore.values
            .sortedBy { it.sequenceNo ?: Long.MAX_VALUE }
        val renderMessages = rawMessages.map { it.toMessageInfo() }
        // 列表内容没有变化时不重复派发，避免 Fragment 反复 submitList 打断滚动。
        if (messageList.value == renderMessages) return
        messageList.value = renderMessages
        if (BuildConfig.DEBUG) {
            println("messageList=---------------------------------------------------------")
            rawMessages.forEachIndexed { index, msg ->
                println("messageList[$index]=${GsonUtils.toJson(msg)}")
            }
        }
    }

    // 同一条服务端消息可能用 messageId/cursor/sequenceNo 再次返回，都映射到同一个本地占位
    private fun JobAgentMessage.rememberServerKeys(localKey: String) {
        listOfNotNull(messageId, cursor, sequenceNo?.toString())
            .filter { it != localKey }
            .forEach { serverUserMessageKeyToLocalKey[it] = localKey }
    }

    /** 将服务端消息映射为端侧渲染模型，messageType 取 [ClawAgentMsgAdapter] 的 TYPE_* itemType */
    private fun JobAgentMessage.toMessageInfo(): ClawAgentMessageInfo {
        val sender = senderType ?: ""
        return ClawAgentMessageInfo(
            id = messageId ?: cursor ?: sequenceNo?.toString() ?: UUID.randomUUID().toString(),
            messageType = resolveItemType(this),
            sendObj = sender,
            receiveObj = if (sender == SENDER_USER) SENDER_AGENT else SENDER_USER,
            content = content,
            metaData = payloadJson,
            messagePhase = messagePhase,
            groupStatus = groupStatus,
            displayPolicy = displayPolicy,
            replaceTargetMessageId = replaceTargetMessageId,
        )
    }

    /** 依据 senderType / messageType / messagePhase / cardType 解析端侧 itemType */
    private fun resolveItemType(msg: JobAgentMessage): Int {
        // 用户消息
        if (msg.senderType == SENDER_USER) {
            return ClawAgentMsgAdapter.TYPE_USER_TEXT
        }
        // 非卡片的处理中/流式消息按思考态展示
        if (msg.messageType != MSG_CARD && (msg.messagePhase == PHASE_PROCESSING || msg.messageStatus == STATUS_STREAMING)) {
            return ClawAgentMsgAdapter.TYPE_AGENT_THINK
        }
        if (msg.messageType == MSG_TEXT_TIP) {
            return ClawAgentMsgAdapter.TYPE_TEXT_TIP
        }
        // 卡片消息按 cardType 分发
        if (msg.messageType == MSG_CARD) {
            return when (msg.cardType) {
                // 引导上传、上传状态
                CARD_INTRODUCE_YOURSELF -> ClawAgentMsgAdapter.TYPE_WELCOME
                CARD_UPLOAD_RESUME -> resolveUploadResumeItemType(msg)
                CARD_HISTORY_RESUME_CONFIRM -> ClawAgentMsgAdapter.TYPE_PENDING_CONFIRM
                CARD_CORE_QUESTION -> resolveQuestionItemType(msg)
                CARD_UNDERSTANDING_SUMMARY -> ClawAgentMsgAdapter.TYPE_UNDERSTANDING_UPGRADE
                CARD_RESUME_RECEIVED -> ClawAgentMsgAdapter.TYPE_UPLOAD_SUCCESS
                CARD_JOB_SEARCHING -> ClawAgentMsgAdapter.TYPE_SEARCHING_JOBS
                CARD_NO_JOB -> ClawAgentMsgAdapter.TYPE_NO_JOBS_FOUND
                CARD_COMPANY_BACKGROUND_ENTRY -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_ENTRY
                CARD_COMPANY_BACKGROUND_REPORT -> resolveCompanyBackgroundReportItemType(msg)
                CARD_MOCK_INTERVIEW_ENTRY -> resolveMockInterviewEntryItemType(msg)
                CARD_MOCK_INTERVIEW_REPORT -> resolveMockInterviewReportItemType(msg)
                CARD_INTERVIEW_PASS_PACKAGE_REPORT -> resolveInterviewPassPackageReportItemType(msg)
                // todo: v1.2 暂时不做
//                CARD_JOB_MATCH_REPORT -> ClawAgentMsgAdapter.TYPE_MATCH_REPORT
//                CARD_TODAY_PROGRESS -> ClawAgentMsgAdapter.TYPE_TODAY_PROGRESS
                else -> ClawAgentMsgAdapter.TYPE_AGENT_TEXT
            }
        }
        // 兜底：Agent 普通文本, 防止适配器报错
        return ClawAgentMsgAdapter.TYPE_AGENT_TEXT
    }

    /** UPLOAD_RESUME 按 resumeUploadStatus 路由：解析成功/失败走独立卡，引导/解析中走欢迎卡（同 messageId upsert 后切卡） */
    private fun resolveUploadResumeItemType(msg: JobAgentMessage): Int {
        val status = msg.payloadJson?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java)?.resumeUploadStatus }.getOrNull()
        }
        return when (status) {
            UPLOAD_STATUS_PARSE_FAILED -> ClawAgentMsgAdapter.TYPE_UPLOAD_FAILED
            UPLOAD_STATUS_WAITING_UPLOAD -> ClawAgentMsgAdapter.TYPE_UPLOAD_WAIT
            else -> ClawAgentMsgAdapter.TYPE_UPLOAD_WAIT // WAITING_UPLOAD / PARSING / null
        }
    }

    private fun resolveQuestionItemType(msg: JobAgentMessage): Int {
        val inputMode = msg.payloadJson?.let {
            runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java)?.inputMode }.getOrNull()
        }
        return when (inputMode) {
            CoreQuestionPayloadBean.MODE_ARRAY_FORM -> ClawAgentMsgAdapter.TYPE_TABLE_FORM
            else -> ClawAgentMsgAdapter.TYPE_QUESTION
        }
    }

    private fun resolveCompanyBackgroundReportItemType(msg: JobAgentMessage): Int {
        // 公司背调卡
        val reportStatus = msg.payloadJson?.let {
            runCatching { GsonUtils.fromJson(it, CompanyBackgroundReportPayloadBean::class.java)?.reportStatus }.getOrNull()
        }
        return when (reportStatus) {
            REPORT_STATUS_FAILED -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_REPORT_FAILED
            REPORT_STATUS_SUCCESS -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_REPORT_DONE
            REPORT_STATUS_GENERATING -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_REPORT_GENERATING
            else -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_REPORT_GENERATING
        }
    }

    private fun resolveMockInterviewEntryItemType(msg: JobAgentMessage): Int {
        // 模拟面试卡
        val entryPayload = msg.payloadJson?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewEntryPayloadBean::class.java) }.getOrNull()
        }
        return if (entryPayload?.entrySource == RESUME_UNDERSTANDING_UPGRADE) {
            ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_ENTRY_UPGRADE
        } else {
            ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_ENTRY
        }
    }

    private fun resolveMockInterviewReportItemType(msg: JobAgentMessage): Int {
        // 模拟面试报告卡
        val defaultPayload = msg.payloadJson?.let {
            runCatching { GsonUtils.fromJson(it, MockInterviewReportPayloadBean::class.java) }.getOrNull()
        }
        return when (defaultPayload?.reportStatus) {
            REPORT_STATUS_FAILED -> ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_REPORT_FAILED
            REPORT_STATUS_SUCCESS -> ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_REPORT_DONE
            REPORT_STATUS_GENERATING -> ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_REPORT_GENERATING
            else -> ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_REPORT_GENERATING
        }
    }

    private fun resolveInterviewPassPackageReportItemType(msg: JobAgentMessage): Int {
        // 面试通关包报告卡
        val payload = msg.payloadJson?.let {
            runCatching { GsonUtils.fromJson(it, PassPackageReportPayloadBean::class.java) }.getOrNull()
        }
        return when (payload?.reportStatus) {
            REPORT_STATUS_FAILED -> ClawAgentMsgAdapter.TYPE_PASS_PACKAGE_REPORT_FAILED
            REPORT_STATUS_SUCCESS -> ClawAgentMsgAdapter.TYPE_PASS_PACKAGE_REPORT_DONE
            REPORT_STATUS_GENERATING -> ClawAgentMsgAdapter.TYPE_PASS_PACKAGE_REPORT_GENERATING
            else -> ClawAgentMsgAdapter.TYPE_PASS_PACKAGE_REPORT_GENERATING
        }
    }

    /** 是否思考态（think）中间态消息：enterSession / 历史分页等终态数据里的此类残留不再展示 */
    private fun JobAgentMessage.isThinkMessage(): Boolean =
        resolveItemType(this) == ClawAgentMsgAdapter.TYPE_AGENT_THINK

    /** 是否TOAST类型卡片 */
    private fun JobAgentMessage.isToastCard(): Boolean = messageType == MSG_TOAST

    /** 是否属性确认卡（ATTRIBUTE_CONFIRM）：走记忆确认弹窗，不进对话列表 */
    private fun JobAgentMessage.isAttributeConfirm(): Boolean = messageType == MSG_CARD && cardType == CARD_ATTRIBUTE_CONFIRM

    /** 是否对齐度达到要求唤起开启通知弹窗，不进对话列表 */
    private fun JobAgentMessage.isNotificationSwitch(): Boolean = messageType == MSG_CARD && cardType == CARD_NOTIFICATION_SWITCH

    /** 是否上传简历卡（UPLOAD_RESUME） */
    private fun JobAgentMessage.isUploadResumeCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_UPLOAD_RESUME

    /** 是否自我介绍卡（CARD_INTRODUCE_YOURSELF） */
    private fun JobAgentMessage.isIntroduceYourselfCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_INTRODUCE_YOURSELF

    /** 是否历史简历确认卡（HISTORY_RESUME_CONFIRM） */
    private fun JobAgentMessage.isHistoryResumeConfirmCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_HISTORY_RESUME_CONFIRM

    /** 是否理解升级（UNDERSTANDING_SUMMARY） */
    private fun JobAgentMessage.isUnderstandingSummaryCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_UNDERSTANDING_SUMMARY

    /** 是否简历流程互斥卡，只保留最新一张 */
    private fun JobAgentMessage.isLatestOnlyResumeCard(): Boolean =
        messageType == MSG_CARD && cardType in setOf(CARD_UPLOAD_RESUME, CARD_RESUME_RECEIVED, CARD_HISTORY_RESUME_CONFIRM)

    /** 是否模拟面试报告生成中（CARD_MOCK_INTERVIEW_REPORT） */
    private fun JobAgentMessage.isMockInterviewReportCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_MOCK_INTERVIEW_REPORT

    /** 是否面试通关包报告生成中（CARD_INTERVIEW_PASS_PACKAGE_REPORT） */
    private fun JobAgentMessage.isPassPackageReportCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_INTERVIEW_PASS_PACKAGE_REPORT

    /**
     * 始终第一条插入自我介绍卡片
     */
    private fun expandIntroduceBeforeUploadResume(messages: List<JobAgentMessage>?): List<JobAgentMessage>? {
        if (messages.isNullOrEmpty()) return messages
        return messages.flatMap { msg ->
            if (msg.sequenceNo == 1L) {
                val introduce = msg.copy(
                    messageId = generateClientMessageId(),
                    cardType = CARD_INTRODUCE_YOURSELF,
                    sequenceNo = 0L
                )
                listOf(introduce, msg)
            } else {
                listOf(msg)
            }
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
        val clientMessageId = generateClientMessageId()
        // 乐观插入：先把用户消息加入列表立即反馈；服务端随后回显由 ingestMessages 插入服务端数据，
        // 但id用本地生成的，用服务端回显消息的话会因为id不同导致刷新两次体验不佳，(跟后端对齐过，这个消息的id只在sse流式中存在，拉历史数据影响不大)
        appendLocalUserMessage(clientMessageId, content)
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
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
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
        streaming.value = false
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
            jobStatusResult.value = data
        }
    }

    /**
     * 更新顶部求职状态
     */
    fun updateJobStatus(statusCode: String?) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty() || statusCode.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
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
            val jobStatusData = jobStatusResult.value
            val selectedOption = jobStatusData?.options.orEmpty().firstOrNull { it.code == statusCode }
            jobStatusResult.value = jobStatusData?.copy(displayValue = selectedOption?.name ?: jobStatusData.displayValue,)
        }
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
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
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
                companyBackgroundResult.value = data
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
                companyBackgroundResult.value = data
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
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
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
            interviewStartResult.value = data
        }
        //interviewStartResult.value = MockInterviewStartData(interviewId = "id666666666", chargeStatus = "DEDUCT_SUCCESS")
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
            interviewPassPackageResumeStatus.postValue(this.data)
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
            interviewUnfinishedResult.value = data
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
                    onSuccess(data)
                }, error = {

                })
        }, onComplete = {
            this.resumeCode.value = null
        })
    }

    /** 获取宝典页业务模块列表，用于动态渲染首页底部快捷入口。 */
    fun getAgentModuleList() {
        request(
            apiBlock = { mRepo.api.queryJobAgentModuleList() },
            showToast = false,
        ) {
            jobAgentModuleList.postValue(this.data.orEmpty())
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
        messageList.value = listOf()
        // 下拉判定条件重置
        historyBeforeCursor = null
        hasMoreHistory = true
        loadingHistory = false
        mockInterviewReportPollingJob?.cancel()
        mockInterviewReportPollingJob = null
        passPackageReportPollingJob?.cancel()
        passPackageReportPollingJob = null
    }

    fun hardPreMsgListCard(list: List<ClawAgentMessageInfo>) {
        messageList.value = list
    }

    override fun onCleared() {
        sseHandler.cancel()
        companyBackgroundGeneratePollingJob?.cancel()
        mockInterviewReportPollingJob?.cancel()
        mockInterviewReportPollingJob = null
        passPackageReportPollingJob?.cancel()
        passPackageReportPollingJob = null
        super.onCleared()
    }

    companion object {
        /** 历史消息分页每页条数 */
        private const val HISTORY_PAGE_SIZE = 20
        /** 公司背调前置校验/报告生成轮询间隔 */
        private const val COMPANY_BACKGROUND_POLL_INTERVAL_MS = 3000L
        // actionType
        private const val INTERACTION_TYPE_TEXT = "TEXT"
        private const val INTERACTION_TYPE_ACTION = "ACTION"

        // 显示开启消息通知弹窗分值
        const val SHOW_DIALOG_SCORE = 85

        // senderType
        const val SENDER_USER = "USER"
        const val SENDER_AGENT = "AGENT"

        // messageType
        private const val MSG_TEXT = "TEXT"
        private const val MSG_CARD = "CARD"
        private const val MSG_TOAST = "TOAST"
        private const val MSG_TEXT_TIP = "TEXT_TIP"

        // messagePhase / messageStatus（思考态判定）
        /** 消息处于处理中 */
        private const val PHASE_PROCESSING = "PROCESSING"
        /** 消息处理失败 */
        private const val PHASE_ERROR = "ERROR"
        /** 消息处理完成并返回结果 */
        private const val PHASE_RESULT = "RESULT"
        /** 消息流式输出中 */
        private const val STATUS_STREAMING = "STREAMING"

        /** 响应组处理失败 */
        private const val GROUP_STATUS_FAILED = "FAILED"
        /** 响应组处理完成 */
        private const val GROUP_STATUS_COMPLETED = "COMPLETED"

        /** APP点击快捷入口后推送 */
        const val QUICK_ENTRY = "QUICK_ENTRY"
        const val ATTRIBUTE_GROUP_JOB_STATUS = "jobStatus"
        /** 小虾对话里理解度升级后推送 */
        const val RESUME_UNDERSTANDING_UPGRADE = "RESUME_UNDERSTANDING_UPGRADE"

        /** 错误码：输入内容不是公司 */
        const val ERROR_CODE_NOT_COMPANY = "NOT_COMPANY"
        /** 错误码：权益点数不足 */
        const val ERROR_CODE_POINTS_INSUFFICIENT = "POINTS_INSUFFICIENT"
        /** 错误码：模型生成失败 */
        const val ERROR_CODE_MODEL_FAILED = "MODEL_FAILED"

        /** 报告生成中 */
        private const val REPORT_STATUS_GENERATING = "GENERATING"
        /** 报告生成成功 */
        private const val REPORT_STATUS_SUCCESS = "SUCCESS"
        /** 报告生成失败 */
        private const val REPORT_STATUS_FAILED = "FAILED"
        /** 报告生成失败，但仍可重试 */
        private const val REPORT_STATUS_FAILED_RETRYABLE = "FAILED_RETRYABLE"
        /** 报告生成最终失败，需停止轮询 */
        private const val REPORT_STATUS_FAILED_FINAL = "FAILED_FINAL"
        /** 面试通关包生成成功 */
        private const val PASS_PACKAGE_STATUS_SUCCESS = 2
        /** 面试通关包生成失败，需停止轮询 */
        private const val PASS_PACKAGE_STATUS_FAILED = 3
        /** 面试通关包生成异常阻断，需停止轮询 */
        private const val PASS_PACKAGE_STATUS_BLOCKED = 4

        // displayPolicy（端侧展示策略）：think 等中间态消息在结果消息到达后的收口方式
        private const val POLICY_REPLACE_ON_TERMINAL = "REPLACE_ON_TERMINAL"
        private const val POLICY_DISMISS_ON_TERMINAL = "DISMISS_ON_TERMINAL"
        private const val POLICY_REPLACE_TARGET = "REPLACE_TARGET"

        /** SSE patch 删除当前内存列表消息 */
        private const val PATCH_TYPE_REMOVE = "REMOVE"

        // resumeUploadStatus：上传简历卡状态机（PARSE_SUCCESS / PARSE_FAILED 路由到独立卡）
        private const val UPLOAD_STATUS_PARSE_SUCCESS = "PARSE_SUCCESS"
        private const val UPLOAD_STATUS_PARSE_FAILED = "PARSE_FAILED"
        private const val UPLOAD_STATUS_WAITING_UPLOAD = "WAITING_UPLOAD"

        // cardType：以下 5 个为接口已定义取值
        private const val CARD_UPLOAD_RESUME = "UPLOAD_RESUME"
        // 收到简历
        private const val CARD_RESUME_RECEIVED = "RESUME_RECEIVED"
        // 历史已上传过简历确认卡片
        private const val CARD_HISTORY_RESUME_CONFIRM = "HISTORY_RESUME_CONFIRM"
        // 问答卡片
        private const val CARD_CORE_QUESTION = "CORE_QUESTION"
        // 记忆卡片
        private const val CARD_ATTRIBUTE_CONFIRM = "ATTRIBUTE_CONFIRM"
        // 对齐度85 唤起消息通知
        private const val CARD_NOTIFICATION_SWITCH = "NOTIFICATION_SWITCH"
        // 理解升级
        private const val CARD_UNDERSTANDING_SUMMARY = "UNDERSTANDING_SUMMARY"
        // 自我介绍
        private const val CARD_INTRODUCE_YOURSELF = "CARD_INTRODUCE_YOURSELF"
        // 公司背调卡片
        private const val CARD_COMPANY_BACKGROUND_ENTRY = "COMPANY_BACKGROUND_ENTRY"
        // 公司背调报告卡片
        private const val CARD_COMPANY_BACKGROUND_REPORT = "COMPANY_BACKGROUND_REPORT"
        // 模拟面试卡片
        private const val CARD_MOCK_INTERVIEW_ENTRY = "MOCK_INTERVIEW_ENTRY"
        // 面试复盘报告卡片
        private const val CARD_MOCK_INTERVIEW_REPORT = "MOCK_INTERVIEW_REPORT"
        //面试通关报告卡片
        private const val CARD_INTERVIEW_PASS_PACKAGE_REPORT = "INTERVIEW_PASS_PACKAGE_REPORT"
        //TODO 以下 cardType 取值为端侧样式占位，需与服务端最终下发的 cardType 对齐后修正

        // 寻找岗位中
        private const val CARD_JOB_SEARCHING = "JOB_SEARCHING"
        // 暂未找到合适岗位
        private const val CARD_NO_JOB = "NO_JOB_TEMPORARILY"
        // 岗位匹配-快速能力提升
        private const val CARD_JOB_MATCH_REPORT = "JOB_MATCH_REPORT"
        // 今日进展卡片 TODO: 后端未给出type
        private const val CARD_TODAY_PROGRESS = "TODAY_PROGRESS"

        /** 强制结束原因：用户主动结束。 */
        private const val FORCE_CANCEL_REASON_USER = "USER_FORCE_CANCEL"
    }
}
