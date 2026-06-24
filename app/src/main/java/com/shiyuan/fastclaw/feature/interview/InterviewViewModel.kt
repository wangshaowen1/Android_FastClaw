package com.shiyuan.fastclaw.feature.interview

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.NetworkUtils
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewAudioTokenBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewStreamBody
import com.shiyuan.fastclaw.core.network.apis.response.InterviewAgentMessage
import com.shiyuan.fastclaw.core.network.apis.response.InterviewAgentPayload
import com.shiyuan.fastclaw.core.network.sse.RawSSEStreamHandler
import com.shiyuan.fastclaw.core.network.sse.processor.InterviewSseProcessor
import com.shiyuan.fastclaw.feature.interview.bean.InterviewRoomMessage
import com.shiyuan.fastclaw.feature.interview.bean.InterviewRoomState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class InterviewViewModel : CommonRepositoryViewModel<AppRepository>() {

    val roomState = MutableLiveData(InterviewRoomState.STARTING)
    val sessionStatus = MutableLiveData(SESSION_STATUS_PREPARING)
    val messageList = MutableLiveData<List<InterviewRoomMessage>>(emptyList())
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
    private val messageStore = LinkedHashMap<String, InterviewRoomMessage>()
    private var answerTimerJob: Job? = null
    private var answerStartTime = 0L

    private var count = 0
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
            },
            onDone = { _, seq ->
                updateLastMessageSeq(seq)
                streaming.value = false
            },
            onComplete = {
                streaming.value = false
            },
            onErrorCallback = {
                //streaming.value = false
                //roomState.value = InterviewRoomState.ERROR

                // 接口未联通时，临时模拟服务端消息，验证完整面试链路。
                streaming.value = false
                count++
                if (count== 1) {

                }
                else if (count > 10) {
                    sseInsertMessages(
                        InterviewAgentMessage(
                            messageId = "mock-interview-finished-${UUID.randomUUID()}",
                            interviewId = mInterviewId,
                            messageSeq = lastMessageSeq + 1,
                            messageType = MSG_INTERVIEW_FINISHED,
                            senderType = SENDER_AGENT,
                            payload = InterviewAgentPayload(
                                sessionStatus = "COMPLETED",
                                roomState = ROOM_STATE_FINISHED,
                                interviewId = mInterviewId,
                                conversationId = mConversationId,
                            ),
                        )
                    )
                } else {
                    sseInsertMessages(
                        InterviewAgentMessage(
                            messageId = "mock-text-done-${UUID.randomUUID()}",
                            interviewId = mInterviewId,
                            messageSeq = lastMessageSeq + 1,
                            messageType = MSG_TEXT_DONE,
                            senderType = SENDER_AGENT,
                            content = "请你先介绍一个最近负责的项目，重点说明你的职责和最终结果。",
                            payload = InterviewAgentPayload(
                                questionType = "MAIN",
                                questionId = "mock-question-001",
                                questionIndex = 1,
                            ),
                        )
                    )
                }
            }
        )
    }

    private val sseHandler by lazy { RawSSEStreamHandler(viewModelScope, sseProcessor) }

    fun enterInterview(interviewId: String?) {
        mInterviewId = interviewId
        queryAudioToken()
        sendAction(ACTION_ENTER_ROOM, emptyMap())
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

    fun startAnswer() {
        val questionId = currentQuestionId ?: return
        roomState.value = InterviewRoomState.ANSWERING
        sendAction(
            actionType = ACTION_ANSWER_START,
            payload = mapOf(
                "questionId" to questionId,
                "answerStartRequestId" to UUID.randomUUID().toString(),
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

    fun sendAction(actionType: String, payload: Map<String, Any?>?) {
        val interviewId = mInterviewId ?: return
        val flow = mRepo.api.mockInterviewStream(
            MockInterviewStreamBody(
                interviewId = interviewId,
                actionType = actionType,
                clientActionId = UUID.randomUUID().toString(),
                lastMessageSeq = lastMessageSeq,
                payload = payload,
                clientSnapshot = buildClientSnapshot(actionType),
            )
        )
        sseHandler.start(flow)
    }

    override fun onCleared() {
        answerTimerJob?.cancel()
        sseHandler.cancel()
        super.onCleared()
    }

    private fun queryAudioToken() {
        request(
            apiBlock = { mRepo.api.queryMockInterviewAudioToken(MockInterviewAudioTokenBody(interviewId = mInterviewId)) },
            showToast = false,
        ) {
            this.data?.ttsToken?.takeIf { it.isNotBlank() }?.let { ttsToken.value = it }
        }
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
        when (message.messageType) {
            MSG_ROOM_STATE_CHANGED -> handleRoomState(message)
            MSG_TEXT_DELTA -> appendAiDelta(message.content.orEmpty())
            MSG_TEXT_DONE -> handleTextDone(message)
            MSG_TEXT_BLOCKED -> handleTextBlocked(message)
            MSG_INTERVIEW_FINISHED -> handleInterviewFinished(message)
            MSG_ERROR -> {
                roomState.value = InterviewRoomState.ERROR
                showToast(message.payload?.errorMessage.orEmpty().ifEmpty { TEXT_INTERVIEW_ERROR })
            }
        }
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
        applyPayloadRoomState(message.payload?.roomState)
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
        message.payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        applyPayloadRoomState(message.payload?.roomState)
        if (message.payload?.roomState.isNullOrBlank()) {
            roomState.value = InterviewRoomState.WAITING_USER_ANSWER
        }
    }

    private fun handleInterviewFinished(message: InterviewAgentMessage) {
        message.payload?.sessionStatus?.takeIf { it.isNotBlank() }?.let { sessionStatus.value = it }
        roomState.value = InterviewRoomState.FINISHED
        finishRoomEvent.value = message.payload?.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
    }

    private fun applyPayloadRoomState(roomStateValue: String?) {
        roomStateValue?.takeIf { it.isNotBlank() }?.let {
            handleRoomState(InterviewAgentMessage(roomState = it))
        }
    }

    private fun appendAiDelta(text: String) {
        if (text.isEmpty()) return
        val targetId = currentAiMessageId
        val old = targetId?.let { messageStore[it] }
        if (old != null) {
            ingestMessages(listOf(old.copy(content = old.content + text)))
        } else {
            val item = InterviewRoomMessage(UUID.randomUUID().toString(), SENDER_AGENT, text)
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


    private fun showToast(text: String) {
        if (text.isNotEmpty()) {
            toastMessage.value = text
        }
    }

    private fun appendMessage(senderType: String, text: String) {
        if (text.isEmpty()) return
        ingestMessages(listOf(InterviewRoomMessage(UUID.randomUUID().toString(), senderType, text)))
    }

    private fun ingestMessages(messages: List<InterviewRoomMessage>?) {
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

    companion object {
        const val ACTION_ENTER_ROOM = "ENTER_ROOM"
        const val ACTION_TARGET_JOB_UPLOAD = "TARGET_JOB_UPLOAD"
        const val ACTION_ANSWER_START = "ANSWER_START"
        const val ACTION_ANSWER_SUBMIT = "ANSWER_SUBMIT"
        const val ANSWER_END_USER_SUBMIT = "USER_SUBMIT"
        const val ANSWER_START_TEXT = "开始作答"
        const val ANSWER_END_TEXT = "点击结束回答"

        private const val TEXT_INTERVIEW_CREATE_FAILED = "模拟面试创建失败"
        private const val TEXT_JOB_UPLOAD_FAILED = "岗位截图上传失败，请重新上传"
        private const val TEXT_INTERVIEW_ERROR = "模拟面试异常，请稍后重试"
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
