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
import java.util.UUID

class InterviewViewModel : CommonRepositoryViewModel<AppRepository>() {

    val roomState = MutableLiveData(InterviewRoomState.STARTING)
    val sessionStatus = MutableLiveData(SESSION_STATUS_PREPARING)
    val messageList = MutableLiveData<List<InterviewMessageInfo>>(emptyList())
    val streaming = MutableLiveData(false)
    val finishRoomEvent = MutableLiveData<String?>()
    val toastMessage = MutableLiveData<String>()
    val answerActionText = MutableLiveData(ANSWER_START_TEXT)
    val answerDurationText = MutableLiveData("")
    val uploadList = MutableLiveData<List<String>>()
    val ttsToken = MutableLiveData<String>()
    val ttsSpeakEvent = MutableLiveData<String>()

    var mInterviewId: String? = null
    var mConversationId: String? = null

    private var lastMessageSeq: Long = 0L
    private var currentQuestionId: String? = null
    private var currentAiMessageId: String? = null
    private val messageStore = LinkedHashMap<String, InterviewMessageInfo>()
    private var answerTimerJob: Job? = null
    private var answerStartTime = 0L

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
                LogUtils.d("sseProcessor_$it")
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

    private val sseHandler by lazy { RawSSEStreamHandler(viewModelScope, sseProcessor) }

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

    fun startAnswer(): Boolean {
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

    fun submitAnswer(answerText: String, endReason: String = ANSWER_END_USER_SUBMIT) {
        val text = answerText.trim()
        if (text.isEmpty()) return
        stopTiming()
        appendMessage(SENDER_USER, text)
        sendAction(
            actionType = ACTION_ANSWER_SUBMIT,
            payload = mapOf(
                "questionId" to currentQuestionId,
                "answerSubmitId" to UUID.randomUUID().toString(),
                "answerText" to text,
                "asrFinalText" to text,
                "answerEndReason" to endReason,
            )
        )
    }

    fun startTiming() {
        answerTimerJob?.cancel()
        answerStartTime = System.currentTimeMillis()
        answerTimerJob = viewModelScope.launch {
            while (isActive) {
                answerActionText.value = ANSWER_END_TEXT
                answerDurationText.value = formatAnswerDuration()
                delay(1000L)
            }
        }
    }

    fun stopTiming() {
        answerTimerJob?.cancel()
        answerTimerJob = null
        answerStartTime = 0L
        answerActionText.value = ANSWER_START_TEXT
        answerDurationText.value = ""
    }

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

    private fun buildClientSnapshot(actionType: String): Map<String, Any?> {
        return mapOf(
            "page" to "MOCK_INTERVIEW_ROOM",
            "clientPhase" to actionType,
            "appVersion" to AppUtils.getAppVersionName(),
            "networkType" to if (NetworkUtils.isWifiConnected()) "WIFI" else "MOBILE",
        )
    }

    fun sseInsertMessages(message: InterviewAgentMessage) {
        message.messageSeq?.let {
            if (it <= lastMessageSeq) return
            lastMessageSeq = it
        }
        message.payload?.latestMessageSeq?.let { updateLastMessageSeq(it) }
        handleRecoverPayload(message)?.let { return }
        when (message.messageType) {
            MSG_ROOM_STATE_CHANGED -> {
                handleRoomState(message)
                handleTextDone(message)
            }
            MSG_TEXT_DELTA ->
                appendAiDelta(message.content.orEmpty())
            MSG_TEXT_DONE -> {
                handleRoomState(message)
                handleTextDone(message)
            }
            MSG_TEXT_BLOCKED -> {
                handleRoomState(message)
                handleTextBlocked(message)
            }
            MSG_INTERVIEW_FINISHED -> {
                handleRoomState(message)
                handleInterviewFinished(message)
                handleTextDone(message)
            }
            MSG_ERROR -> {
                roomState.value = InterviewRoomState.ERROR
                val errorMessage = message.errorMessage ?: message.payload?.errorMessage ?: TEXT_INTERVIEW_ERROR
                showToast(errorMessage)
            }
        }
    }

    private fun handleRecoverPayload(message: InterviewAgentMessage): Unit? {
        return when (message.payload?.recoverAction) {
            RECOVER_ACTION_RESTORE_ROOM -> {
                restoreRoom(message)
                Unit
            }
            RECOVER_ACTION_RETURN_TO_CHAT -> {
                handleRoomState(message)
                finishRoomEvent.value = message.payload.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
                Unit
            }
            RECOVER_ACTION_SESSION_CLOSED -> {
                handleRoomState(message)
                showToast(TEXT_INTERVIEW_CLOSED)
                finishRoomEvent.value = message.payload.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
                Unit
            }
            else -> null
        }
    }

    private fun restoreRoom(message: InterviewAgentMessage) {
        val payload = message.payload
        payload?.messages.orEmpty().forEachIndexed { index, item ->
            Log.d(
                TAG,
                "restoreRoom_message[$index]: messageId=${item.messageId}, messageSeq=${item.messageSeq}, messageType=${item.messageType}, senderType=${item.senderType}, roomState=${item.roomState ?: item.payload?.roomState}, questionId=${item.payload?.questionId}, content=${item.content.orEmpty()}"
            )
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

        payload?.currentQuestion
            ?.takeIf { question ->
                question.questionId == currentQuestionId &&
                    question.content?.isNotBlank() == true &&
                    restoredMessages.none { it.senderType == SENDER_AGENT && it.content == question.content }
            }
            ?.content
            ?.let { appendMessage(SENDER_AGENT, it) }

        handleRoomState(message)
    }

    private fun updateLastMessageSeq(seq: Long?) {
        seq?.takeIf { it > lastMessageSeq }?.let { lastMessageSeq = it }
    }

    private fun handleRoomState(event: InterviewAgentMessage) {
        event.payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        when (event.payload?.roomState ?: event.roomState) {
            ROOM_STATE_WAITING_TARGET_JOB -> roomState.value = InterviewRoomState.WAITING_TARGET_JOB
            ROOM_STATE_TARGET_JOB_PARSING -> roomState.value = InterviewRoomState.TARGET_JOB_PARSING
            ROOM_STATE_TARGET_JOB_PARSE_SUCCESS -> roomState.value = InterviewRoomState.TARGET_JOB_PARSE_SUCCESS
            ROOM_STATE_TARGET_JOB_PARSE_FAILED -> roomState.value = InterviewRoomState.TARGET_JOB_PARSE_FAILED
            ROOM_STATE_PLAN_GENERATING -> roomState.value = InterviewRoomState.PLAN_GENERATING
            ROOM_STATE_PLAN_READY -> roomState.value = InterviewRoomState.PLAN_READY
            ROOM_STATE_WAITING_USER_ANSWER -> roomState.value = InterviewRoomState.WAITING_USER_ANSWER
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

    private fun handleTextDone(message: InterviewAgentMessage) {
        message.payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        currentQuestionId = message.payload?.questionId?.takeIf { it.isNotEmpty() } ?: currentQuestionId
        when (message.senderType) {
            SENDER_AGENT -> {
                message.content?.takeIf { it.isNotEmpty() }?.let {
                    replaceCurrentAiMessage(it)
                    ttsSpeakEvent.value = it
                }
                if (message.payload?.roomState.isNullOrBlank()) {
                    roomState.value = InterviewRoomState.WAITING_USER_ANSWER
                }
            }
            SENDER_USER -> {
                // User answer has already been inserted locally.
            }
        }
        currentAiMessageId = null
    }

    private fun handleTextBlocked(message: InterviewAgentMessage) {
        message.content?.takeIf { it.isNotEmpty() }?.let { appendMessage(SENDER_AGENT, it) }
        /*message.payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        if (message.payload?.roomState.isNullOrBlank()) {
            roomState.value = InterviewRoomState.WAITING_USER_ANSWER
        }*/
    }

    private fun handleInterviewFinished(message: InterviewAgentMessage) {
        /*message.payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        roomState.value = InterviewRoomState.FINISHED*/
        finishRoomEvent.value = message.payload?.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
    }

    private fun appendAiDelta(text: String) {
        if (text.isEmpty()) return
        val targetId = currentAiMessageId
        val old = targetId?.let { messageStore[it] }
        if (old != null) {
            ingestMessages(listOf(old.copy(content = old.content + text)))
        } else {
            val item = InterviewMessageInfo(UUID.randomUUID().toString(), SENDER_AGENT, text)
            currentAiMessageId = item.id
            ingestMessages(listOf(item))
        }
    }

    private fun replaceCurrentAiMessage(text: String) {
        val targetId = currentAiMessageId
        val old = targetId?.let { messageStore[it] }
        if (old != null) {
            ingestMessages(listOf(old.copy(content = text)))
        } else {
            appendMessage(SENDER_AGENT, text)
        }
    }

    private fun appendMessage(senderType: String, text: String) {
        if (text.isEmpty()) return
        ingestMessages(listOf(InterviewMessageInfo(UUID.randomUUID().toString(), senderType, text)))
    }

    private fun InterviewAgentMessage.toRoomMessage(): InterviewMessageInfo? {
        val text = content?.takeIf { it.isNotBlank() } ?: return null
        val sender = senderType?.takeIf { it.isNotBlank() } ?: SENDER_AGENT
        val localId = messageId
            ?: messageSeq?.toString()
            ?: UUID.randomUUID().toString()
        return InterviewMessageInfo(localId, sender, text)
    }

    private fun List<InterviewAgentMessage>.lastQuestionId(): String? {
        return sortedWith(
            compareBy<InterviewAgentMessage> { it.messageSeq ?: Long.MIN_VALUE }
                .thenBy { it.messageId.orEmpty() }
        ).asReversed()
            .firstNotNullOfOrNull { it.payload?.questionId?.takeIf { questionId -> questionId.isNotBlank() } }
    }

    private fun ingestMessages(messages: List<InterviewMessageInfo>?) {
        if (messages.isNullOrEmpty()) return
        messages.forEach { messageStore[it.id] = it }
        refreshMessageList()
    }

    private fun refreshMessageList() {
        messageList.value = messageStore.values.toList()
    }

    private fun formatAnswerDuration(): String {
        val seconds = ((System.currentTimeMillis() - answerStartTime).coerceAtLeast(0L) / 1000L).toInt()
        return "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun queryAudioToken() {
        request(
                apiBlock = { mRepo.api.queryMockInterviewAudioToken(MockInterviewAudioTokenBody(interviewId = mInterviewId)) },
                showToast = false,
        ) {
            this.data?.ttsToken?.takeIf { it.isNotBlank() }?.let { ttsToken.value = it }
        }
    }

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
            showToastError = true,
        ) {
            roomState.value = InterviewRoomState.CANCELED
            sseHandler.cancel()
            stopTiming()
            onSuccess(this.data)
        }
    }

    private fun showToast(text: String) {
        if (text.isNotEmpty()) {
            toastMessage.value = text
        }
    }

    override fun onCleared() {
        answerTimerJob?.cancel()
        sseHandler.cancel()
        super.onCleared()
    }

    companion object {
        private const val TAG = "InterviewViewModel"
        const val ACTION_ENTER_ROOM = "ENTER_ROOM"
        const val ACTION_TARGET_JOB_SUBMIT = "TARGET_JOB_SUBMIT"
        const val TARGET_JOB_INPUT_UPLOAD_IMAGE = "UPLOAD_IMAGE"
        const val TARGET_JOB_INPUT_POSITION_SELECTED = "POSITION_SELECTED"
        const val ACTION_ANSWER_START = "ANSWER_START"
        const val ACTION_ANSWER_SUBMIT = "ANSWER_SUBMIT"
        const val ACTION_SESSION_RECOVER = "SESSION_RECOVER"
        const val ANSWER_END_USER_SUBMIT = "USER_SUBMIT"
        const val ANSWER_START_TEXT = "开始作答"
        const val ANSWER_END_TEXT = "点击结束回答"

        private const val TEXT_INTERVIEW_CREATE_FAILED = "模拟面试创建失败"
        private const val TEXT_JOB_UPLOAD_FAILED = "岗位截图上传失败，请重新上传"
        private const val TEXT_INTERVIEW_ERROR = "模拟面试异常，请稍后重试"
        private const val TEXT_INTERVIEW_CLOSED = "当前面试已关闭"
        private const val TEXT_QUESTION_NOT_READY = "当前题目未就绪，请稍后再试"
        private const val RECOVER_SCENE_ENTRY_CLICK = "ENTRY_CLICK"
        private const val RECOVER_ACTION_RESTORE_ROOM = "RESTORE_ROOM"
        private const val RECOVER_ACTION_RETURN_TO_CHAT = "RETURN_TO_CHAT"
        private const val RECOVER_ACTION_SESSION_CLOSED = "SESSION_CLOSED"
        private const val FORCE_CANCEL_REASON_USER = "USER_FORCE_CANCEL"
        const val MSG_ROOM_STATE_CHANGED = "ROOM_STATE_CHANGED"
        const val MSG_TEXT_DELTA = "TEXT_DELTA"
        const val MSG_TEXT_DONE = "TEXT_DONE"
        const val MSG_TEXT_BLOCKED = "TEXT_BLOCKED"
        const val MSG_INTERVIEW_FINISHED = "INTERVIEW_FINISHED"
        const val MSG_ERROR = "ERROR"
        const val SENDER_AGENT = "AGENT"
        const val SENDER_USER = "USER"

        const val ROOM_STATE_WAITING_TARGET_JOB = "WAITING_TARGET_JOB"
        const val ROOM_STATE_TARGET_JOB_PARSING = "TARGET_JOB_PARSING"
        const val ROOM_STATE_TARGET_JOB_PARSE_SUCCESS = "TARGET_JOB_PARSE_SUCCESS"
        const val ROOM_STATE_TARGET_JOB_PARSE_FAILED = "TARGET_JOB_PARSE_FAILED"
        const val ROOM_STATE_PLAN_GENERATING = "PLAN_GENERATING"
        const val ROOM_STATE_PLAN_READY = "PLAN_READY"
        const val ROOM_STATE_WAITING_USER_ANSWER = "WAITING_USER_ANSWER"
        const val ROOM_STATE_ANSWER_SUBMITTED = "ANSWER_SUBMITTED"
        const val ROOM_STATE_WAITING_FOLLOW_UP_DECISION = "WAITING_FOLLOW_UP_DECISION"
        const val ROOM_STATE_REPORT_GENERATING = "REPORT_GENERATING"
        const val ROOM_STATE_FINISHED = "FINISHED"
        const val ROOM_STATE_CANCELED = "CANCELED"

        const val SESSION_STATUS_PREPARING = "PREPARING"
    }
}
