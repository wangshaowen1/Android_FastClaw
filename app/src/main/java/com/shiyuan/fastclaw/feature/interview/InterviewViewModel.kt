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
    val messageList = MutableLiveData<List<InterviewRoomMessage>>(emptyList())
    val streaming = MutableLiveData(false)
    val finishRoomEvent = MutableLiveData<String?>()
    val toastMessage = MutableLiveData<String>()
    val answerActionText = MutableLiveData(ANSWER_START_TEXT)
    val answerDurationText = MutableLiveData("")
    val uploadList = MutableLiveData<List<String>>()

    var mInterviewId: String? = null
    var mConversationId: String? = null

    private var lastMessageSeq: Long = 0L
    private var currentQuestionId: String? = null
    private var currentAiMessageId: String? = null
    private val messageStore = LinkedHashMap<String, InterviewRoomMessage>()
    private var started = false
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

                // 接口未联通时，临时模拟一条服务端 TEXT_DONE 消息，验证页面展示链路。
                streaming.value = false
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
                            nextActionHint = NEXT_WAIT_USER_START_ANSWER,
                        ),
                    )
                )
            }
        )
    }

    private val sseHandler by lazy { RawSSEStreamHandler(viewModelScope, sseProcessor) }

    fun enterInterview(interviewId: String?) {
        mInterviewId = interviewId
        sendAction(ACTION_ENTER_ROOM, emptyMap())
    }

    fun uploadTargetJob(uris: List<Uri>) {
        if (uris.isEmpty()) return
        roomState.value = InterviewRoomState.TARGET_JOB_PARSING
        setLoading(true)
        submitMultipleFiles(uris) { successUrls, _ ->
            setLoading(false)
            if (successUrls.isEmpty()) {
                showToast(TEXT_JOB_UPLOAD_FAILED)
                roomState.value = InterviewRoomState.WAITING_TARGET_JOB
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
        appendUserMessage(text)
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

    fun exitInterview() {
        sendAction(
            actionType = ACTION_SESSION_EXIT,
            payload = mapOf(
                "exitRequestId" to UUID.randomUUID().toString(),
                "cancelReason" to "USER_EXIT",
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
            // Audio SDK token is read from data when ASR/TTS is initialized.
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

    private fun sseInsertMessages(event: InterviewAgentMessage) {
        event.messageSeq?.let {
            if (it <= lastMessageSeq) return
            lastMessageSeq = it
        }
        when (event.messageType) {
            MSG_ROOM_STATE_CHANGED -> handleRoomState(event)
            MSG_TEXT_DELTA -> appendAiDelta(event.content.orEmpty())
            MSG_TEXT_DONE -> handleTextDone(event)
            MSG_TEXT_BLOCKED -> handleTextBlocked(event)
            MSG_INTERVIEW_FINISHED -> handleInterviewFinished(event)
            MSG_ERROR -> {
                roomState.value = InterviewRoomState.ERROR
                showToast(event.payload?.errorMessage.orEmpty().ifEmpty { TEXT_INTERVIEW_ERROR })
            }
        }
    }

    private fun updateLastMessageSeq(seq: Long?) {
        seq?.takeIf { it > lastMessageSeq }?.let { lastMessageSeq = it }
    }

    private fun handleRoomState(event: InterviewAgentMessage) {
        when (event.roomState ?: event.payload?.roomState) {
            "TARGET_JOB_PARSING" -> roomState.value = InterviewRoomState.TARGET_JOB_PARSING
            "TARGET_JOB_PARSE_FAILED" -> roomState.value = InterviewRoomState.WAITING_TARGET_JOB
            "PLAN_GENERATING", "PLAN_READY" -> roomState.value = InterviewRoomState.PREPARING
            "WAITING_USER_ANSWER" -> roomState.value = InterviewRoomState.INTERVIEWING
            "CANCELED" -> finishRoomEvent.value = mConversationId
        }
    }

    private fun handleTextDone(event: InterviewAgentMessage) {
        currentQuestionId = event.payload?.questionId?.takeIf { it.isNotEmpty() } ?: currentQuestionId
        when (event.senderType) {
            SENDER_AGENT -> {
                event.content?.takeIf { it.isNotEmpty() }?.let { replaceCurrentAiMessage(it) }
                roomState.value = InterviewRoomState.INTERVIEWING
            }
            SENDER_USER -> {
                // User answer has already been inserted locally.
            }
        }
        if (event.payload?.nextActionHint == NEXT_WAIT_USER_START_ANSWER) {
            roomState.value = InterviewRoomState.INTERVIEWING
        }
        currentAiMessageId = null
    }

    private fun handleTextBlocked(event: InterviewAgentMessage) {
        event.content?.takeIf { it.isNotEmpty() }?.let { appendMessage(SENDER_AGENT, it) }
        roomState.value = InterviewRoomState.INTERVIEWING
    }

    private fun handleInterviewFinished(event: InterviewAgentMessage) {
        roomState.value = InterviewRoomState.FINISHED
        finishRoomEvent.value = event.payload?.conversationId?.takeIf { it.isNotEmpty() } ?: mConversationId
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

    private fun appendUserMessage(text: String) = appendMessage(SENDER_USER, text)

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
        const val ACTION_SESSION_EXIT = "SESSION_EXIT"
        const val ANSWER_END_USER_SUBMIT = "USER_SUBMIT"
        const val ANSWER_START_TEXT = "开始作答"
        const val ANSWER_END_TEXT = "点击结束回答"

        private const val TEXT_INTERVIEW_CREATE_FAILED = "模拟面试创建失败"
        private const val TEXT_JOB_UPLOAD_FAILED = "岗位截图上传失败，请重新上传"
        private const val TEXT_INTERVIEW_ERROR = "模拟面试异常，请稍后重试"
        private const val MSG_ROOM_STATE_CHANGED = "ROOM_STATE_CHANGED"
        private const val MSG_TEXT_DELTA = "TEXT_DELTA"
        private const val MSG_TEXT_DONE = "TEXT_DONE"
        private const val MSG_TEXT_BLOCKED = "TEXT_BLOCKED"
        private const val MSG_INTERVIEW_FINISHED = "INTERVIEW_FINISHED"
        private const val MSG_ERROR = "ERROR"
        const val SENDER_AGENT = "AGENT"
        const val SENDER_USER = "USER"
        private const val NEXT_WAIT_USER_START_ANSWER = "WAIT_USER_START_ANSWER"
    }
}
