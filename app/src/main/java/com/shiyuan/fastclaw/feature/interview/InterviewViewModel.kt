package com.shiyuan.fastclaw.feature.interview

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewAudioTokenBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewForceCancelBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewStreamBody
import com.shiyuan.fastclaw.core.network.apis.response.InterviewAgentMessage
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewForceCancelData
import com.shiyuan.fastclaw.core.network.sse.RawSSEStreamHandler
import com.shiyuan.fastclaw.core.network.sse.processor.InterviewSseProcessor
import com.shiyuan.fastclaw.feature.interview.bean.InterviewMessageInfo
import com.shiyuan.fastclaw.feature.interview.bean.InterviewRoomState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.UUID

/**
 * 模拟面试房间 ViewModel。
 *
 * 负责面试间 SSE 交互、房间状态流转、消息列表合并、目标岗位提交、语音作答提交和强制结束面试。
 */
class InterviewViewModel : CommonRepositoryViewModel<AppRepository>() {

    /** 当前房间状态，用于驱动底部操作区展示。 */
    val roomState = MutableLiveData(InterviewRoomState.STARTING)

    /** 当前会话状态，用于驱动页面标题和状态标签。 */
    val sessionStatus = MutableLiveData(SESSION_STATUS_PREPARING)

    /** 面试房间消息列表。 */
    val messageList = MutableLiveData<List<InterviewMessageInfo>>(emptyList())

    /** SSE 是否正在处理。 */
    val streaming = MutableLiveData(false)

    /** 面试结束后通知页面关闭并回到对应小虾对话。 */
    val finishRoomEvent = MutableLiveData<String?>()

    /** 收到 INTERVIEW_FINISHED 后，等待结束语音播报完成再关闭页面。 */
    val finishAfterTtsEvent = MutableLiveData<String?>()

    /** 页面 Toast 文案。 */
    val toastMessage = MutableLiveData<String>()

    /** 底部语音按钮文案。 */
    val answerActionText = MutableLiveData(ANSWER_START_TEXT)

    /** 当前作答计时文本。 */
    val answerDurationText = MutableLiveData("")

    /** 当前作答倒计时结束事件。 */
    val answerTimeoutEvent = MutableLiveData<Unit>()

    /** 目标岗位截图上传成功后的 OSS key 列表。 */
    val uploadList = MutableLiveData<List<String>>()

    /** TTS 临时 token。 */
    val ttsToken = MutableLiveData<String>()

    /** 需要播放的 TTS 文本事件。 */
    val ttsSpeakEvent = MutableLiveData<String>()

    /** 需要流式播报的 TTS 增量文本事件。 */
    val ttsStreamDeltaEvent = MutableLiveData<String>()

    /** TTS 流式文本结束事件。 */
    val ttsStreamFinishEvent = MutableLiveData<Unit>()

    /** Agent 完整问题播报完成后，自动打开用户作答。 */
    val autoStartAnswerAfterTtsEvent = MutableLiveData<Unit>()

    /** 当前模拟面试会话 ID。 */
    var mInterviewId: String? = null

    /** 面试结束后回到的小虾对话 ID。 */
    var mConversationId: String? = null

    /** 端侧已处理的最后一条面试消息序号。 */
    private var lastMessageSeq: Long = 0L

    /** 当前正在回答或等待回答的问题 ID。 */
    private var currentQuestionId: String? = null

    /** 已提交答案后等待服务端返回下一题的题目 ID。 */
    private var submittedQuestionId: String? = null

    /** 当前流式 AI 消息本地 ID，用于 TEXT_DELTA 追加更新。 */
    private var currentAiMessageId: String? = null

    /** 当前流式 AI 消息是否已经发送过 TTS 文本。 */
    private var currentAiTtsEnabled = false

    /** 当前用户语音作答草稿消息 ID，用于 ASR partial 流式更新。 */
    private var currentUserDraftMessageId: String? = null

    /** 端侧兜底流式展示完整 Agent 文本的任务。 */
    private var localAiStreamJob: Job? = null

    /** 端侧兜底流式展示完整 Agent 文本的待播队列。 */
    private val localAiStreamQueue = ArrayDeque<LocalAiStreamMessage>()

    /** 收到面试结束后，等待本地结束语流式展示和 TTS 输入完成再通知页面关闭。 */
    private var pendingFinishAfterTtsConversationId: String? = null

    /** 按消息 ID 保存的端侧消息缓存。 */
    private val messageStore = LinkedHashMap<String, InterviewMessageInfo>()

    /** 作答倒计时任务。 */
    private var answerTimerJob: Job? = null

    /** 模拟面试 SSE 事件处理器。 */
    private val sseProcessor by lazy {
        InterviewSseProcessor(
            onStart = {
                streaming.value = true
            },
            onMeta = { _, seq ->
                updateLastMessageSeq(seq)
            },
            onMessage = {
                sseInsertMessages(it)
                LogUtils.d("sseProcessor_onMessage$it")
            },
            onDone = { _, seq ->
                updateLastMessageSeq(seq)
                streaming.value = false
            },
            onComplete = {
                streaming.value = false
            },
            onErrorCallback = {
                streaming.value = false
                roomState.value = InterviewRoomState.ERROR
            }
        )
    }

    /** 模拟面试 SSE 流处理器。 */
    private val sseHandler by lazy { RawSSEStreamHandler(viewModelScope, sseProcessor) }

    /**
     * 进入或恢复模拟面试房间。
     */
    fun enterOrRecover(interviewId: String?, isRecover: Boolean) {
        mInterviewId = interviewId
        queryAudioToken()
        if (!isRecover) {
            sendAction(ACTION_ENTER_ROOM, emptyMap())
        } else {
            sendAction(
                actionType = ACTION_SESSION_RECOVER,
                payload = mapOf(
                    "recoverScene" to RECOVER_SCENE_ENTRY_CLICK,
                )
            )
        }
    }

    /**
     * 上传目标岗位截图，成功后由页面观察 uploadList 提交目标岗位动作。
     */
    fun uploadTarget(uris: List<Uri>) {
        if (uris.isEmpty()) return
        roomState.value = InterviewRoomState.TARGET_JOB_PARSING
        setLoading(true)
        submitMultipleFiles(uris) { successUrls, _ ->
            setLoading(false)
            if (successUrls.isEmpty()) {
                showToast(TEXT_JOB_UPLOAD_FAILED)
                roomState.value = InterviewRoomState.TARGET_JOB_PARSE_FAILED
                return@submitMultipleFiles
            }
            uploadList.value = successUrls
        }
    }

    /**
     * 开始当前题作答，并向服务端发送 ANSWER_START。
     */
    fun startAnswer(): Boolean {
        if (submittedQuestionId != null) return false
        val questionId = currentQuestionId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        currentQuestionId = questionId
        roomState.value = InterviewRoomState.ANSWERING
        sendAction(
            actionType = ACTION_ANSWER_START,
            payload = mapOf(
                "questionId" to questionId,
                "answerStartRequestId" to UUID.randomUUID().toString(),
            )
        )
        return true
    }

    /**
     * 提交用户作答文本，并用同一个 answerSubmitId 关联本地消息与服务端请求。
     */
    fun submitAnswer(answerText: String, endReason: String = ANSWER_END_USER_SUBMIT) {
        val text = answerText.trim()
        if (text.isEmpty()) return
        stopTiming()
        submittedQuestionId = currentQuestionId
        roomState.value = InterviewRoomState.ANSWER_SUBMITTED
        val answerSubmitId = UUID.randomUUID().toString()
        val draftId = currentUserDraftMessageId
        val draft = draftId?.let { messageStore[it] }
        if (draft != null) {
            messageStore[draft.id.orEmpty()] = draft.copy(content = text, answerSubmitId = answerSubmitId)
            currentUserDraftMessageId = null
            refreshMessageList()
        } else {
            appendMessage(
                InterviewMessageInfo(
                    id = UUID.randomUUID().toString(),
                    senderType = SENDER_USER,
                    content = text,
                    answerSubmitId = answerSubmitId,
                )
            )
        }
        sendAction(
            actionType = ACTION_ANSWER_SUBMIT,
            payload = mapOf(
                "questionId" to currentQuestionId,
                "answerSubmitId" to answerSubmitId,
                "answerText" to text,
                "asrFinalText" to text,
                "answerEndReason" to endReason,
            )
        )
    }

    /**
     * 开始作答倒计时。
     */
    fun startTiming() {
        answerTimerJob?.cancel()
        answerTimerJob = viewModelScope.launch {
            var remainingSeconds = ANSWER_COUNTDOWN_SECONDS
            while (isActive && remainingSeconds >= 0) {
                answerActionText.value = ANSWER_END_TEXT
                answerDurationText.value = formatAnswerDuration(remainingSeconds)
                if (remainingSeconds == 0) {
                    answerTimeoutEvent.value = Unit
                    break
                }
                delay(1000L)
                remainingSeconds--
            }
        }
    }

    /**
     * 流式更新用户语音作答草稿；点击结束回答后再由 submitAnswer 真正提交后台。
     */
    fun updateUserAnswerDraft(text: String) {
        val content = text.trim()
        if (content.isEmpty()) {
            clearUserAnswerDraft()
            return
        }
        val draftId = currentUserDraftMessageId
        val draft = draftId?.let { messageStore[it] }
        if (draft != null) {
            ingestMessages(listOf(draft.copy(content = content)))
        } else {
            val item = InterviewMessageInfo(
                id = UUID.randomUUID().toString(),
                senderType = SENDER_USER,
                content = content,
            )
            currentUserDraftMessageId = item.id
            ingestMessages(listOf(item))
        }
    }

    /**
     * 清理未提交的用户语音作答草稿。
     */
    fun clearUserAnswerDraft() {
        currentUserDraftMessageId?.let { messageStore.remove(it) }
        currentUserDraftMessageId = null
        refreshMessageList()
    }

    /**
     * 停止作答倒计时并恢复按钮文案。
     */
    fun stopTiming() {
        answerTimerJob?.cancel()
        answerTimerJob = null
        answerActionText.value = ANSWER_START_TEXT
        answerDurationText.value = ""
    }

    /**
     * 发送面试间动作 SSE 请求。
     */
    fun sendAction(actionType: String, payload: Map<String, Any?>?) {
        val flow = mRepo.api.mockInterviewStream(
            MockInterviewStreamBody(
                interviewId = mInterviewId,
                actionType = actionType,
                clientActionId = UUID.randomUUID().toString(),
                lastMessageSeq = lastMessageSeq,
                payload = payload,
                clientSnapshot = buildClientSnapshot(actionType),
            )
        )
        sseHandler.start(flow)
    }

    /**
     * 构建服务端动作请求所需的端侧快照。
     */
    private fun buildClientSnapshot(actionType: String): Map<String, Any?> {
        return mapOf(
            "page" to "MOCK_INTERVIEW_ROOM",
            "clientPhase" to actionType,
            "appVersion" to AppUtils.getAppVersionName(),
            "networkType" to if (NetworkUtils.isWifiConnected()) "WIFI" else "MOBILE",
        )
    }

    /**
     * 处理单条模拟面试 SSE 消息，并分发到对应业务处理逻辑。
     */
    fun sseInsertMessages(message: InterviewAgentMessage) {
        // lastMessageSeq 仅作为下一次请求的水位，不在这里拦截业务消息；
        // onMeta / payload.latestMessageSeq 可能先推进水位，同序号消息仍要继续分发处理。
        updateLastMessageSeq(message.messageSeq)
        message.payload?.latestMessageSeq?.let { updateLastMessageSeq(it) }
        if (!message.payload?.recoverAction.isNullOrBlank() && handleRecoverPayload(message)) return
        when (message.messageType) {
            MSG_ROOM_STATE_CHANGED -> { //房间状态变化
                handleRoomState(message)
                handleTextDone(message)
            }
            MSG_TEXT_DELTA -> //文本流式片段
                appendAiDelta(message.content.orEmpty(), message.isTtsEnabled())
            MSG_TEXT_DONE -> { //文本完整输出
                handleRoomState(message)
                handleTextDone(message)
            }
            MSG_TEXT_BLOCKED -> { //内容安全拦截文本
                handleRoomState(message)
                handleTextBlocked(message)
            }
            MSG_INTERVIEW_FINISHED -> { //面试结束
                handleTextDone(message)
                handleRoomState(message)
                handleInterviewFinished(message)
            }
            MSG_ERROR -> { //错误
                roomState.value = InterviewRoomState.ERROR
                val errorMessage = message.errorMessage ?: message.payload?.errorMessage ?: TEXT_INTERVIEW_ERROR
                showToast(errorMessage)
            }
        }
    }

    /**
     * 处理恢复房间相关 payload，命中恢复动作时拦截后续普通消息处理。
     */
    private fun handleRecoverPayload(message: InterviewAgentMessage): Boolean {
        return when (message.payload?.recoverAction) {
            RECOVER_ACTION_RESTORE_ROOM -> {
                restoreRoom(message)
                true
            }
            RECOVER_ACTION_RETURN_TO_CHAT -> {
                handleRoomState(message)
                finishRoomEvent.value = message.payload.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
                true
            }
            RECOVER_ACTION_SESSION_CLOSED -> {
                handleRoomState(message)
                showToast(TEXT_INTERVIEW_CLOSED)
                finishRoomEvent.value = message.payload.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
                true
            }
            else -> false
        }
    }

    /**
     * 恢复面试房间现场：还原历史消息、最新题目、消息序号和房间状态。
     */
    private fun restoreRoom(message: InterviewAgentMessage) {
        val payload = message.payload
        payload?.messages.orEmpty().forEachIndexed { index, item ->
            LogUtils.d("restoreRoom_$item")
        }
        Log.d(TAG, "restoreRoom_---------------------------------------")
        payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        payload?.latestMessageSeq?.let { updateLastMessageSeq(it) }
        currentQuestionId = payload?.messages?.lastQuestionId()
            ?: payload?.currentQuestion?.questionId?.takeIf { it.isNotBlank() }
            ?: currentQuestionId

        val restoredMessages = payload?.messages
            ?.sortedBy { it.messageSeq ?: Long.MAX_VALUE }
            ?.mapNotNull { it.toRoomMessage() }
            .orEmpty()

        messageStore.clear()
        currentAiMessageId = null
        if (restoredMessages.isEmpty()) {
            refreshMessageList()
        } else {
            ingestMessages(restoredMessages)
        }

        payload?.currentQuestion?.takeIf { question ->
            question.questionId == currentQuestionId && question.content?.isNotBlank() == true && restoredMessages.none {
                it.senderType == SENDER_AGENT && it.content == question.content
            }
        }?.content?.let {
            appendMessage(
                InterviewMessageInfo(
                    id = UUID.randomUUID().toString(),
                    senderType = SENDER_AGENT,
                    content = it,
                )
            )
        }

        handleRoomState(message)
    }

    /**
     * 根据服务端 roomState/sessionStatus 更新端侧房间状态。
     */
    private fun handleRoomState(event: InterviewAgentMessage) {
        event.payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        when (event.payload?.roomState ?: event.roomState) {
            ROOM_STATE_WAITING_TARGET_JOB -> roomState.value = InterviewRoomState.WAITING_TARGET_JOB
            ROOM_STATE_TARGET_JOB_PARSING -> roomState.value = InterviewRoomState.TARGET_JOB_PARSING
            ROOM_STATE_TARGET_JOB_PARSE_SUCCESS -> roomState.value = InterviewRoomState.TARGET_JOB_PARSE_SUCCESS
            ROOM_STATE_TARGET_JOB_PARSE_FAILED -> roomState.value = InterviewRoomState.TARGET_JOB_PARSE_FAILED
            ROOM_STATE_PLAN_GENERATING -> roomState.value = InterviewRoomState.PLAN_GENERATING
            ROOM_STATE_PLAN_READY -> roomState.value = InterviewRoomState.PLAN_READY
            ROOM_STATE_WAITING_USER_ANSWER -> updateWaitingUserAnswerState(event.payload?.questionId)
            ROOM_STATE_ANSWER_SUBMITTED -> roomState.value = InterviewRoomState.ANSWER_SUBMITTED
            ROOM_STATE_WAITING_FOLLOW_UP_DECISION -> roomState.value = InterviewRoomState.WAITING_FOLLOW_UP_DECISION
            ROOM_STATE_REPORT_GENERATING -> {
                roomState.value = InterviewRoomState.REPORT_GENERATING
                finishRoomEvent.value = event.payload?.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
            }
            ROOM_STATE_FINISHED -> {
                roomState.value = InterviewRoomState.FINISHED
                finishRoomEvent.value = event.payload?.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
            }
            ROOM_STATE_CANCELED -> {
                roomState.value = InterviewRoomState.CANCELED
                finishRoomEvent.value = event.payload?.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
            }
        }
    }

    /**
     * 处理完整文本消息，Agent 文本用于展示和 TTS，User 文本由本地提交时已插入。
     */
    private fun handleTextDone(message: InterviewAgentMessage) {
        message.payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        message.payload?.questionId?.takeIf { it.isNotEmpty() }?.let {
            updateQuestionId(it)
        }
        when (message.senderType) {
            SENDER_AGENT -> {
                val hasStreamingMessage = currentAiMessageId != null
                val shouldAutoStartAfterTts = if (hasStreamingMessage) currentAiTtsEnabled else message.isTtsEnabled()
                message.content?.takeIf { it.isNotEmpty() }?.let {
                    if (hasStreamingMessage) {
                        replaceCurrentAiMessage(it)
                    } else {
                        streamFullAiMessage(it, message.isTtsEnabled())
                    }
                    if (shouldAutoStartAfterTts) {
                        autoStartAnswerAfterTtsEvent.value = Unit
                    }
                }
                if (hasStreamingMessage && currentAiTtsEnabled) {
                    ttsStreamFinishEvent.value = Unit
                }
                if (message.payload?.roomState.isNullOrBlank()) {
                    updateWaitingUserAnswerState(message.payload?.questionId)
                }
            }
            SENDER_USER -> {
                // User answer has already been inserted locally.
            }
        }
        currentAiMessageId = null
        currentAiTtsEnabled = false
    }

    /**
     * 处理内容安全拦截文本；优先按 answerSubmitId 替换本地用户回答。
     */
    private fun handleTextBlocked(message: InterviewAgentMessage) {
        val blockedText = message.content?.takeIf { it.isNotEmpty() }
        val replaced = replaceBlockedAnswerMessage(message)
        if (!replaced) {
            blockedText?.let {
                appendMessage(InterviewMessageInfo(id = UUID.randomUUID().toString(), senderType = SENDER_AGENT, content = it))
            }
        }
        blockedText?.let {
            if (message.isTtsEnabled()) {
                ttsSpeakEvent.value = it
            }
        }
        if (replaced || (message.payload?.roomState ?: message.roomState) == ROOM_STATE_WAITING_USER_ANSWER) {
            submittedQuestionId = null
            updateWaitingUserAnswerState(message.payload?.questionId ?: currentQuestionId)
        }
    }

    /**
     * 处理面试结束事件，通知页面等待结束语音播报完成后退出面试房间。
     */
    private fun handleInterviewFinished(message: InterviewAgentMessage) {
        pendingFinishAfterTtsConversationId = message.payload?.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
        notifyFinishAfterTtsIfReady()
    }

    /**
     * 追加 Agent 流式文本片段。
     */
    private fun appendAiDelta(text: String, ttsEnabled: Boolean) {
        if (text.isEmpty()) return
        localAiStreamJob?.cancel()
        localAiStreamJob = null
        localAiStreamQueue.clear()
        if (ttsEnabled) {
            currentAiTtsEnabled = true
            ttsStreamDeltaEvent.value = text
        }
        val targetId = currentAiMessageId
        val old = targetId?.let { messageStore[it] }
        if (old != null) {
            ingestMessages(listOf(old.copy(content = old.content.orEmpty() + text)))
        } else {
            val item = InterviewMessageInfo(UUID.randomUUID().toString(), SENDER_AGENT, text)
            currentAiMessageId = item.id
            ingestMessages(listOf(item))
        }
    }

    /**
     * 后端直接返回完整 Agent 文本时，端侧拆成小块模拟流式展示和流式播报。
     */
    private fun streamFullAiMessage(text: String, ttsEnabled: Boolean) {
        val chunks = text.chunked(LOCAL_AI_STREAM_CHUNK_SIZE)
        if (chunks.isEmpty()) return
        localAiStreamQueue.offer(LocalAiStreamMessage(UUID.randomUUID().toString(), chunks, ttsEnabled))
        if (localAiStreamJob?.isActive == true) return
        startLocalAiStreamQueue()
    }

    /**
     * 顺序消费完整 Agent 文本队列，避免连续多条服务端消息互相打断。
     */
    private fun startLocalAiStreamQueue() {
        localAiStreamJob = viewModelScope.launch {
            while (isActive && localAiStreamQueue.isNotEmpty()) {
                val streamMessage = localAiStreamQueue.poll() ?: continue
                val ttsBuilder = StringBuilder()
                streamMessage.chunks.forEachIndexed { index, chunk ->
                    if (index == 0) {
                        val item = InterviewMessageInfo(streamMessage.id, SENDER_AGENT, chunk)
                        ingestMessages(listOf(item))
                    } else {
                        delay(LOCAL_AI_STREAM_DELAY_MS)
                        appendLocalAiStreamChunk(streamMessage.id, chunk)
                    }
                    if (streamMessage.ttsEnabled) {
                        ttsBuilder.append(chunk)
                        if (shouldFlushTtsChunk(ttsBuilder)) {
                            flushTtsChunk(ttsBuilder)
                        }
                    }
                }
                if (streamMessage.ttsEnabled) {
                    flushTtsChunk(ttsBuilder)
                }
            }
            ttsStreamFinishEvent.value = Unit
            localAiStreamJob = null
            if (localAiStreamQueue.isNotEmpty()) {
                startLocalAiStreamQueue()
            } else {
                notifyFinishAfterTtsIfReady()
            }
        }
    }

    /**
     * 本地结束语流式展示完成后，再通知页面等待 TTS 播完并关闭。
     */
    private fun notifyFinishAfterTtsIfReady() {
        val conversationId = pendingFinishAfterTtsConversationId ?: return
        if (localAiStreamJob?.isActive == true || localAiStreamQueue.isNotEmpty()) return
        pendingFinishAfterTtsConversationId = null
        finishAfterTtsEvent.value = conversationId
    }

    /**
     * 追加端侧兜底流式文本片段到当前 Agent 消息。
     */
    private fun appendLocalAiStreamChunk(messageId: String, text: String) {
        val old = messageStore[messageId] ?: return
        ingestMessages(listOf(old.copy(content = old.content.orEmpty() + text)))
    }

    /**
     * TTS 输入不能按单字过碎发送，积累到短句或标点后再交给播放器。
     */
    private fun shouldFlushTtsChunk(builder: StringBuilder): Boolean {
        if (builder.length >= LOCAL_AI_TTS_CHUNK_SIZE) return true
        return builder.lastOrNull() in LOCAL_AI_TTS_FLUSH_CHARS
    }

    /**
     * 发送并清空端侧流式 TTS 缓冲。
     */
    private fun flushTtsChunk(builder: StringBuilder) {
        val text = builder.toString()
        if (text.isNotEmpty()) {
            ttsStreamDeltaEvent.value = text
            builder.clear()
        }
    }

    /**
     * payload.ttsEnabled 为空时保持旧逻辑默认播报，只有明确 false 才不播报。
     */
    private fun InterviewAgentMessage.isTtsEnabled(): Boolean {
        return payload?.ttsEnabled != false
    }

    /**
     * 更新端侧已处理的最后消息序号。
     */
    private fun updateLastMessageSeq(seq: Long?) {
        seq?.takeIf { it > lastMessageSeq }?.let { lastMessageSeq = it }
    }

    /**
     * 更新当前题目 ID；如果服务端返回的是新题，解除上一题已提交后的等待态。
     */
    private fun updateQuestionId(questionId: String) {
        if (submittedQuestionId != null && questionId != submittedQuestionId) {
            submittedQuestionId = null
        }
        currentQuestionId = questionId
    }

    /**
     * 进入等待用户作答前先校验是否已经回答过当前题；未拿到新题时继续展示等待态。
     */
    private fun updateWaitingUserAnswerState(questionId: String?) {
        questionId?.takeIf { it.isNotBlank() }?.let { updateQuestionId(it) }
        roomState.value = if (submittedQuestionId == null) {
            InterviewRoomState.WAITING_USER_ANSWER
        } else {
            InterviewRoomState.ANSWER_SUBMITTED
        }
    }

    /**
     * 用完整文本替换当前流式 AI 消息。
     */
    private fun replaceCurrentAiMessage(text: String) {
        val targetId = currentAiMessageId
        val old = targetId?.let { messageStore[it] }
        if (old != null) {
            ingestMessages(listOf(old.copy(content = text)))
        } else {
            appendMessage(
                InterviewMessageInfo(
                    id = UUID.randomUUID().toString(),
                    senderType = SENDER_AGENT,
                    content = text,
                )
            )
        }
    }

    /**
     * 追加单条端侧消息。
     */
    private fun appendMessage(message: InterviewMessageInfo) {
        message.content?.let { ingestMessages(listOf(message)) }
    }

    /**
     * 服务端返回 TEXT_BLOCKED 时，按 answerSubmitId 替换对应的本地用户回答内容。
     */
    private fun replaceBlockedAnswerMessage(message: InterviewAgentMessage): Boolean {
        val blockedText = message.content?.takeIf { it.isNotBlank() } ?: return false
        val answerSubmitId = message.payload?.answerSubmitId?.takeIf { it.isNotBlank() } ?: return false
        val blockedRoomState = message.payload.roomState ?: message.roomState
        if (blockedRoomState != ROOM_STATE_WAITING_USER_ANSWER) return false
        val target = messageStore.entries.firstOrNull { it.value.answerSubmitId == answerSubmitId } ?: return false
        target.setValue(target.value.copy(content = blockedText))
        refreshMessageList()
        return true
    }

    /**
     * 将服务端面试消息转换为端侧列表消息。
     */
    private fun InterviewAgentMessage.toRoomMessage(): InterviewMessageInfo? {
        val text = content?.takeIf { it.isNotBlank() } ?: return null
        val sender = senderType?.takeIf { it.isNotBlank() } ?: SENDER_AGENT
        val localId = messageId
            ?: messageSeq?.toString()
            ?: UUID.randomUUID().toString()
        return InterviewMessageInfo(
            id = localId,
            senderType = sender,
            content = text,
            answerSubmitId = payload?.answerSubmitId,
        )
    }

    /**
     * 从历史消息中查找最后一条带 questionId 的题目 ID。
     */
    private fun List<InterviewAgentMessage>.lastQuestionId(): String? {
        return sortedWith(
            compareBy<InterviewAgentMessage> { it.messageSeq ?: Long.MIN_VALUE }
                .thenBy { it.messageId.orEmpty() }
        ).asReversed()
            .firstNotNullOfOrNull { it.payload?.questionId?.takeIf { questionId -> questionId.isNotBlank() } }
    }

    /**
     * 合并消息缓存并刷新列表。
     */
    private fun ingestMessages(messages: List<InterviewMessageInfo>?) {
        if (messages.isNullOrEmpty()) return
        messages.forEach { messageStore[it.id?:""] = it }
        refreshMessageList()
    }

    /**
     * 将消息缓存回推给页面列表。
     */
    private fun refreshMessageList() {
        messageList.value = messageStore.values.toList()
    }

    /**
     * 格式化当前作答剩余时长。
     */
    private fun formatAnswerDuration(remainingSeconds: Int): String {
        val seconds = remainingSeconds.coerceAtLeast(0)
        return "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    /**
     * 查询模拟面试音频 token，用于 TTS 初始化。
     */
    private fun queryAudioToken() {
        request(
                apiBlock = { mRepo.api.queryMockInterviewAudioToken(MockInterviewAudioTokenBody(interviewId = mInterviewId)) },
                showToast = false,
        ) {
            this.data?.ttsToken?.takeIf { it.isNotBlank() }?.let { ttsToken.value = it }
        }
    }

    /**
     * 强制结束当前模拟面试。
     */
    fun forceFinish(onSuccess: (data: MockInterviewForceCancelData?) -> Unit) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.forceCancelMockInterview(
                    MockInterviewForceCancelBody(
                        interviewId = mInterviewId,
                        forceCancelRequestId = UUID.randomUUID().toString(),
                        cancelReason = FORCE_CANCEL_REASON_USER,
                    )
                )
            },
            onError = {
                onSuccess(null)
            },
            showToastError = true,
        ) {
            roomState.value = InterviewRoomState.CANCELED
            sseHandler.cancel()
            stopTiming()
            onSuccess(this.data)
        }
    }

    /**
     * 发送 Toast 文案到页面。
     */
    private fun showToast(text: String) {
        if (text.isNotEmpty()) {
            toastMessage.value = text
        }
    }

    /**
     * 清理计时任务和 SSE 连接。
     */
    override fun onCleared() {
        answerTimerJob?.cancel()
        localAiStreamJob?.cancel()
        sseHandler.cancel()
        super.onCleared()
    }

    /**
     * 完整服务端文本的端侧流式展示任务。
     */
    private data class LocalAiStreamMessage(
        val id: String,
        val chunks: List<String>,
        val ttsEnabled: Boolean,
    )

    companion object {
        /** 日志标签。 */
        private const val TAG = "InterviewViewModel"
        /** 动作：进入面试房间。 */
        const val ACTION_ENTER_ROOM = "ENTER_ROOM"
        /** 动作：提交目标岗位。 */
        const val ACTION_TARGET_JOB_SUBMIT = "TARGET_JOB_SUBMIT"
        /** 目标岗位输入方式：上传岗位截图。 */
        const val TARGET_JOB_INPUT_UPLOAD_IMAGE = "UPLOAD_IMAGE"
        /** 目标岗位输入方式：选择岗位。 */
        const val TARGET_JOB_INPUT_POSITION_SELECTED = "POSITION_SELECTED"
        /** 动作：开始作答。 */
        const val ACTION_ANSWER_START = "ANSWER_START"
        /** 动作：提交作答。 */
        const val ACTION_ANSWER_SUBMIT = "ANSWER_SUBMIT"
        /** 动作：恢复面试会话。 */
        const val ACTION_SESSION_RECOVER = "SESSION_RECOVER"
        /** 作答结束原因：用户主动提交。 */
        const val ANSWER_END_USER_SUBMIT = "USER_SUBMIT"
        /** 作答结束原因：倒计时结束自动提交。 */
        const val ANSWER_END_TIMEOUT_AUTO_SUBMIT = "TIMEOUT_AUTO_SUBMIT"
        /** 单题作答倒计时秒数。 */
        private const val ANSWER_COUNTDOWN_SECONDS = 180
        /** 完整服务端文本兜底流式展示的单块长度。 */
        private const val LOCAL_AI_STREAM_CHUNK_SIZE = 1
        /** 完整服务端文本兜底流式展示的间隔。 */
        private const val LOCAL_AI_STREAM_DELAY_MS = 52L
        /** 完整服务端文本兜底流式播报的最小发送长度。 */
        private const val LOCAL_AI_TTS_CHUNK_SIZE = 8
        /** 完整服务端文本兜底流式播报遇到这些符号时立即发送。 */
        private val LOCAL_AI_TTS_FLUSH_CHARS = setOf('，', '。', '？', '！', '；', ',', '.', '?', '!', ';', '\n')
        /** 底部按钮文案：开始作答。 */
        const val ANSWER_START_TEXT = "开始作答"
        /** 底部按钮文案：结束当前回答。 */
        const val ANSWER_END_TEXT = "点击结束回答"
        /** 创建面试失败提示。 */
        private const val TEXT_INTERVIEW_CREATE_FAILED = "模拟面试创建失败"
        /** 岗位截图上传失败提示。 */
        private const val TEXT_JOB_UPLOAD_FAILED = "岗位截图上传失败，请重新上传"
        /** 面试通用异常提示。 */
        private const val TEXT_INTERVIEW_ERROR = "模拟面试异常，请稍后重试"
        /** 面试已关闭提示。 */
        private const val TEXT_INTERVIEW_CLOSED = "当前面试已关闭"
        /** 当前题目未准备好提示。 */
        private const val TEXT_QUESTION_NOT_READY = "当前题目未就绪，请稍后再试"
        /** 恢复场景：点击入口进入。 */
        private const val RECOVER_SCENE_ENTRY_CLICK = "ENTRY_CLICK"
        /** 恢复动作：恢复面试房间现场。 */
        private const val RECOVER_ACTION_RESTORE_ROOM = "RESTORE_ROOM"
        /** 恢复动作：返回小虾对话。 */
        private const val RECOVER_ACTION_RETURN_TO_CHAT = "RETURN_TO_CHAT"
        /** 恢复动作：会话已关闭。 */
        private const val RECOVER_ACTION_SESSION_CLOSED = "SESSION_CLOSED"
        /** 强制结束原因：用户主动结束。 */
        private const val FORCE_CANCEL_REASON_USER = "USER_FORCE_CANCEL"
        /** SSE 消息类型：房间状态变化。 */
        const val MSG_ROOM_STATE_CHANGED = "ROOM_STATE_CHANGED"
        /** SSE 消息类型：文本流式片段。 */
        const val MSG_TEXT_DELTA = "TEXT_DELTA"
        /** SSE 消息类型：文本完整输出。 */
        const val MSG_TEXT_DONE = "TEXT_DONE"
        /** SSE 消息类型：内容安全拦截文本。 */
        const val MSG_TEXT_BLOCKED = "TEXT_BLOCKED"
        /** SSE 消息类型：面试结束。 */
        const val MSG_INTERVIEW_FINISHED = "INTERVIEW_FINISHED"
        /** SSE 消息类型：错误。 */
        const val MSG_ERROR = "ERROR"
        /** 消息发送方：面试官/Agent。 */
        const val SENDER_AGENT = "AGENT"
        /** 消息发送方：用户。 */
        const val SENDER_USER = "USER"
        /** 房间状态：等待目标岗位。 */
        const val ROOM_STATE_WAITING_TARGET_JOB = "WAITING_TARGET_JOB"
        /** 房间状态：目标岗位解析中。 */
        const val ROOM_STATE_TARGET_JOB_PARSING = "TARGET_JOB_PARSING"
        /** 房间状态：目标岗位解析成功。 */
        const val ROOM_STATE_TARGET_JOB_PARSE_SUCCESS = "TARGET_JOB_PARSE_SUCCESS"
        /** 房间状态：目标岗位解析失败。 */
        const val ROOM_STATE_TARGET_JOB_PARSE_FAILED = "TARGET_JOB_PARSE_FAILED"
        /** 房间状态：面试计划生成中。 */
        const val ROOM_STATE_PLAN_GENERATING = "PLAN_GENERATING"
        /** 房间状态：面试计划已生成。 */
        const val ROOM_STATE_PLAN_READY = "PLAN_READY"
        /** 房间状态：等待用户作答。 */
        const val ROOM_STATE_WAITING_USER_ANSWER = "WAITING_USER_ANSWER"
        /** 房间状态：回答已提交。 */
        const val ROOM_STATE_ANSWER_SUBMITTED = "ANSWER_SUBMITTED"
        /** 房间状态：等待追问判断。 */
        const val ROOM_STATE_WAITING_FOLLOW_UP_DECISION = "WAITING_FOLLOW_UP_DECISION"
        /** 房间状态：报告生成中。 */
        const val ROOM_STATE_REPORT_GENERATING = "REPORT_GENERATING"
        /** 房间状态：面试已完成。 */
        const val ROOM_STATE_FINISHED = "FINISHED"
        /** 房间状态：面试已取消。 */
        const val ROOM_STATE_CANCELED = "CANCELED"
        /** 会话状态：准备中。 */
        const val SESSION_STATUS_PREPARING = "PREPARING"
    }
}
