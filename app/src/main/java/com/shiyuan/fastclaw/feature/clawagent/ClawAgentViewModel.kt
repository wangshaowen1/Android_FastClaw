package com.shiyuan.fastclaw.feature.clawagent

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.network.apis.request.ActionSubmitBody
import com.shiyuan.fastclaw.core.network.apis.request.AgentMsgAction
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
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentMessage
import com.shiyuan.fastclaw.core.network.apis.response.MessagePageData
import com.shiyuan.fastclaw.core.network.apis.response.MessagePullData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeConfirmData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.SessionEnterData
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.core.network.sse.RawSSEStreamHandler
import com.shiyuan.fastclaw.core.network.sse.processor.ClawAgentSseProcessor
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

    /** 当前会话 ID，发送消息/提交动作时回传 */
    private var sessionId: String? = null

    /** 当前对话 ID */
    private var conversationId: String? = null

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
    private var currentRequestId: String? = null

    /** 已接收的最后一条 SSE 事件序号，用于断线重连 lastSeq */
    private var lastSeq: Long? = null

    /** 本地乐观插入的用户消息 ID，待服务端回显后把服务端字段填充进同一占位（保留 id） */
    private val optimisticUserMessageIds = LinkedHashSet<String>()

    /** 记录是否初始化会话id, 用于判断是否拉取增量数据 */
    val hasInitSession = MutableLiveData<Boolean>(false)

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
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
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
            // 插入数据
            ingestMessages(result.messages?.filterNot { it.isThinkMessage() })
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
        // 未就绪 / 加载中 / 没有更早消息：直接结束本次（下拉）
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty() || loadingHistory) {
            return
        }
        loadingHistory = true
        val beforeCursor = historyBeforeCursor
            ?: messageStore.values.minByOrNull { it.sequenceNo ?: Long.MAX_VALUE }?.cursor
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
            onError = {
                loadingHistory = false
            },
        ) {
            loadingHistory = false
            val result = this.data
            historyMessages.value = result
            result?.nextBeforeCursor?.let { historyBeforeCursor = it }
            hasMoreHistory = result?.hasMore ?: false
            // 更早消息 sequenceNo 更小，ingest 后按序排到列表顶部（DiffUtil 在 0 位插入，不跳动）
            // 历史分页为终态数据，过滤掉残留的思考态(think)中间态消息；并在 UPLOAD_RESUME 卡前补自我介绍卡
            ingestMessages(expandIntroduceBeforeUploadResume(result?.messages?.filterNot { it.isThinkMessage() }))
            // 已加载到最早一页时给一次轻提示
//            if (!hasMoreHistory) R.string.no_more_data_pull.getResString().showToast()
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
                runCatching { GsonUtils.fromJson(newMsg.payloadJson, UploadResumePayloadBean::class.java) }.getOrNull()
            }
            payload?.interactionState?.let { state ->
                // 未消费，消费过的卡片不再弹
                if (state.disabled == false) {
                    attributeConfirmPopup.value = newMsg.toMessageInfo()
                }
            }
        }
        ingestMessages(listOf(newMsg))
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
    private fun ingestMessages(messages: List<JobAgentMessage>?) {
        if (messages.isNullOrEmpty()) return
        messages.forEach { msg ->
            // ATTRIBUTE_CONFIRM 走记忆确认弹窗，不进对话列表（enterSession / 历史分页 / 增量拉取统一过滤）
            if (msg.isAttributeConfirm()) return@forEach
            val key = msg.messageId ?: msg.cursor ?: msg.sequenceNo?.toString() ?: return@forEach
            // 服务端回显的用户消息：把服务端字段填充进本地乐观占位，但保留占位的 messageId
            // 以维持 item 身份（id 不变 → 不触发 remove+insert，仅静默 change），避免气泡刷两次
            if (msg.senderType == SENDER_USER && key !in optimisticUserMessageIds) {
                val localKey = optimisticUserMessageIds.firstOrNull { messageStore[it]?.content == msg.content }
                if (localKey != null) {
                    messageStore[localKey] = msg.copy(messageId = localKey)
                    optimisticUserMessageIds.remove(localKey)
                    // 已填充进占位，不再以服务端 messageId 另存，避免重复
                    // TODO 若后续 /message/pull 或 /message/page 又以真实 messageId 返回本条，会产生重复，按需再做 id 映射
                    return@forEach
                }
            }
            messageStore[key] = msg
        }
        refreshMessageList()
    }

    // region 消息列表相关

    /** 按 sequenceNo 升序映射并回推渲染列表 */
    private fun refreshMessageList() {
        messageList.value = messageStore.values
            .sortedBy { it.sequenceNo ?: Long.MAX_VALUE }
            .map { it.toMessageInfo() }
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
        )
    }

    /** 依据 senderType / messageType / messagePhase / cardType 解析端侧 itemType */
    private fun resolveItemType(msg: JobAgentMessage): Int {
        // 用户消息
        if (msg.senderType == SENDER_USER) return ClawAgentMsgAdapter.TYPE_USER_TEXT
        // 非卡片的处理中/流式消息按思考态展示
        if (msg.messageType != MSG_CARD &&
            (msg.messagePhase == PHASE_PROCESSING || msg.messageStatus == STATUS_STREAMING)
        ) {
            return ClawAgentMsgAdapter.TYPE_AGENT_THINK
        }
        // 卡片消息按 cardType 分发
        if (msg.messageType == MSG_CARD) {
            return when (msg.cardType) {
                // 引导上传、上传状态
                CARD_INTRODUCE_YOURSELF -> ClawAgentMsgAdapter.TYPE_WELCOME
                CARD_UPLOAD_RESUME -> resolveUploadResumeItemType(msg)
                CARD_HISTORY_RESUME_CONFIRM -> ClawAgentMsgAdapter.TYPE_PENDING_CONFIRM
                CARD_CORE_QUESTION -> ClawAgentMsgAdapter.TYPE_QUESTION
                CARD_UNDERSTANDING_SUMMARY -> ClawAgentMsgAdapter.TYPE_UNDERSTANDING_UPGRADE
                CARD_RESUME_RECEIVED -> ClawAgentMsgAdapter.TYPE_UPLOAD_SUCCESS
                CARD_JOB_SEARCHING -> ClawAgentMsgAdapter.TYPE_SEARCHING_JOBS
                CARD_NO_JOB -> ClawAgentMsgAdapter.TYPE_NO_JOBS_FOUND
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

    /** 是否思考态（think）中间态消息：enterSession / 历史分页等终态数据里的此类残留不再展示 */
    private fun JobAgentMessage.isThinkMessage(): Boolean =
        resolveItemType(this) == ClawAgentMsgAdapter.TYPE_AGENT_THINK

    /** 是否属性确认卡（ATTRIBUTE_CONFIRM）：走记忆确认弹窗，不进对话列表 */
    private fun JobAgentMessage.isAttributeConfirm(): Boolean = messageType == MSG_CARD && cardType == CARD_ATTRIBUTE_CONFIRM

    /** 是否上传简历卡（UPLOAD_RESUME） */
    private fun JobAgentMessage.isUploadResumeCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_UPLOAD_RESUME

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

    // endregion

    // endregion

    // region 对话SSE流相关

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
        val flow = mRepo.api.sendMsgToAgent(
            JobAgentStreamBody(
                sessionId = currentSessionId,
                conversationId = currentConversationId,
                clientMessageId = clientMessageId,
                content = content,
                interactionType = INTERACTION_TYPE_TEXT
            )
        )
        sseHandler.start(flow)
    }

    /**
     * 卡片交互 - sse流式
     */
    fun sendAction(desc: String, actionData: AgentMsgAction) {
        val currentSessionId = sessionId
        val currentConversationId = conversationId
        if (currentSessionId.isNullOrEmpty() || currentConversationId.isNullOrEmpty()) {
            streamError.value = IllegalStateException("会话未就绪，请先进入会话")
            return
        }
        val clientMessageId = generateClientMessageId()
        val flow = mRepo.api.sendMsgToAgent(
            JobAgentStreamBody(
                sessionId = currentSessionId,
                conversationId = currentConversationId,
                clientMessageId = clientMessageId,
                content = desc,
                interactionType = INTERACTION_TYPE_ACTION,
                action = actionData
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
     * 退出登录 用来重置
     */
    fun reset() {
        sseHandler.cancel()
        sessionId = null
        conversationId = null
        latestCursor = null
        messageStore.clear()
        messageList.value = listOf()
    }

    override fun onCleared() {
        sseHandler.cancel()
        super.onCleared()
    }

    companion object {
        /** 历史消息分页每页条数 */
        private const val HISTORY_PAGE_SIZE = 20
        // actionType
        private const val INTERACTION_TYPE_TEXT = "TEXT"
        private const val INTERACTION_TYPE_ACTION = "ACTION"

        // senderType
        private const val SENDER_USER = "USER"
        private const val SENDER_AGENT = "AGENT"

        // messageType
        private const val MSG_TEXT = "TEXT"
        private const val MSG_CARD = "CARD"

        // messagePhase / messageStatus（思考态判定）
        private const val PHASE_PROCESSING = "PROCESSING"
        private const val STATUS_STREAMING = "STREAMING"

        // displayPolicy（端侧展示策略）：think 等中间态消息在结果消息到达后的收口方式
        private const val POLICY_REPLACE_ON_TERMINAL = "REPLACE_ON_TERMINAL"
        private const val POLICY_DISMISS_ON_TERMINAL = "DISMISS_ON_TERMINAL"

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
        // 理解升级
        private const val CARD_UNDERSTANDING_SUMMARY = "UNDERSTANDING_SUMMARY"
        // 自我介绍
        private const val CARD_INTRODUCE_YOURSELF = "CARD_INTRODUCE_YOURSELF"
        //TODO 以下 cardType 取值为端侧样式占位，需与服务端最终下发的 cardType 对齐后修正

        // 寻找岗位中
        private const val CARD_JOB_SEARCHING = "JOB_SEARCHING"
        // 暂未找到合适岗位
        private const val CARD_NO_JOB = "NO_JOB"
        // 岗位匹配-快速能力提升
        private const val CARD_JOB_MATCH_REPORT = "JOB_MATCH_REPORT"
        // 今日进展卡片 TODO: 后端未给出type
        private const val CARD_TODAY_PROGRESS = "TODAY_PROGRESS"
    }
}