package com.shiyuan.fastclaw.feature.interview

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.NetworkUtils
import com.google.gson.JsonObject
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewAudioTokenBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewStartBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewStreamBody
import com.shiyuan.fastclaw.core.network.apis.response.InterviewAgentMessage
import com.shiyuan.fastclaw.core.network.sse.RawSSEStreamHandler
import com.shiyuan.fastclaw.core.network.sse.processor.InterviewSseProcessor
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

    var interviewId: String? = null
        private set
    var conversationId: String? = null
        private set

    private var lastMessageSeq: Long = 0L
    private var currentQuestionId: String? = null
    private var currentAiMessageId: String? = null
    private val messageStore = LinkedHashMap<String, InterviewRoomMessage>()
    private var started = false
    private var answerTimerJob: Job? = null
    private var answerStartTime = 0L

    private val sseProcessor by lazy {
        InterviewSseProcessor(
            onStart = { streaming.value = true },
            onMeta = { _, seq -> updateLastMessageSeq(seq) },
            onMessage = { sseInsertMessages(it) },
            onDone = { _, seq ->
                updateLastMessageSeq(seq)
                streaming.value = false
            },
            onComplete = { streaming.value = false },
            onErrorCallback = {
                streaming.value = false
                roomState.value = InterviewRoomState.ERROR
            }
        )
    }

    private val sseHandler by lazy { RawSSEStreamHandler(viewModelScope, sseProcessor) }

    fun uploadTargetJob(uris: List<Uri>) {
        if (uris.isEmpty()) return
        roomState.value = InterviewRoomState.TARGET_JOB_PARSING
        setLoading(true)
        submitMultipleFiles(uris) { successUrls, _ ->
            setLoading(false)
            if (successUrls.isEmpty()) {
                showToast("岗位截图上传失败，请重新上传")
                roomState.value = InterviewRoomState.WAITING_TARGET_JOB
                return@submitMultipleFiles
            }
            sendStreamAction(
                actionType = ACTION_TARGET_JOB_UPLOAD,
                payload = mapOf(
                    "uploadBatchId" to UUID.randomUUID().toString(),
                    "imageOssKeys" to successUrls,
                )
            )
        }
    }

    fun startInterview(entryMessageId: String?, entrySource: String?, resumeId: String?) {
        if (started) return
        started = true
        requestWithLoading(
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
            showToastError = true,
        ) {
            val result = data ?: return@requestWithLoading
            if (result.isDeductSuccess()) {
                interviewId = result.interviewId
                conversationId = result.conversationId
                roomState.value = InterviewRoomState.PREPARING
                queryAudioToken()
                enterRoom()
            } else {
                roomState.value = InterviewRoomState.ERROR
                showToast(result.message ?: "模拟面试创建失败")
            }
        }
    }

    fun startAnswer() {
        val questionId = currentQuestionId ?: return
        roomState.value = InterviewRoomState.ANSWERING
        sendStreamAction(
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
                // 每次开始作答都重新计时，按钮上同步展示本轮作答时长。
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
        sendStreamAction(
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
        sendStreamAction(
            actionType = ACTION_SESSION_EXIT,
            payload = mapOf(
                "exitRequestId" to UUID.randomUUID().toString(),
                "cancelReason" to "USER_EXIT",
            )
        )
    }

    override fun onCleared() {
        answerTimerJob?.cancel()
        sseHandler.cancel()
        super.onCleared()
    }

    private fun enterRoom() {
        sendStreamAction(ACTION_ENTER_ROOM, emptyMap())
    }

    private fun queryAudioToken() {
        request(
            apiBlock = { mRepo.api.queryMockInterviewAudioToken(MockInterviewAudioTokenBody(interviewId = interviewId)) },
            showToast = false,
        ) {
            // ASR/TTS SDK 接入时从 data 读取临时 token。
        }
    }

    private fun sendStreamAction(actionType: String, payload: Map<String, Any?>?) {
        val id = interviewId ?: return
        sseHandler.start(
            mRepo.api.mockInterviewStream(
                MockInterviewStreamBody(
                    interviewId = id,
                    actionType = actionType,
                    clientActionId = UUID.randomUUID().toString(),
                    lastMessageSeq = lastMessageSeq,
                    payload = payload,
                    clientSnapshot = buildClientSnapshot(actionType),
                )
            )
        )
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
            "ROOM_STATE_CHANGED" -> handleRoomState(event)
            "AI_TEXT_DELTA" -> appendAiDelta(event.payload.optString("text"))
            "AI_TEXT_DONE" -> handleAiTextDone(event)
            "INTERVIEW_FINISHED" -> handleInterviewFinished(event)
            "ERROR" -> {
                roomState.value = InterviewRoomState.ERROR
                showToast(event.payload.optString("errorMessage").ifEmpty { "模拟面试异常，请稍后重试" })
            }
        }
    }

    private fun updateLastMessageSeq(seq: Long?) {
        seq?.takeIf { it > lastMessageSeq }?.let { lastMessageSeq = it }
    }

    private fun handleRoomState(event: InterviewAgentMessage) {
        when (event.payload.optString("roomState")) {
            "TARGET_JOB_PARSING" -> {
                roomState.value = InterviewRoomState.TARGET_JOB_PARSING
            }
            "TARGET_JOB_PARSE_FAILED" -> roomState.value = InterviewRoomState.WAITING_TARGET_JOB
            "PLAN_GENERATING", "PLAN_READY" -> roomState.value = InterviewRoomState.PREPARING
            "WAITING_USER_ANSWER" -> roomState.value = InterviewRoomState.INTERVIEWING
            "CANCELED" -> finishRoomEvent.value = conversationId
        }
    }

    private fun handleAiTextDone(event: InterviewAgentMessage) {
        currentQuestionId = event.payload.optString("questionId").takeIf { it.isNotEmpty() } ?: currentQuestionId
        event.payload.optString("fullText").takeIf { it.isNotEmpty() }?.let { fullText ->
            replaceCurrentAiMessage(fullText)
        }
        if (event.payload.optString("nextActionHint") == "WAIT_USER_START_ANSWER") {
            roomState.value = InterviewRoomState.INTERVIEWING
        }
        currentAiMessageId = null
    }

    private fun handleInterviewFinished(event: InterviewAgentMessage) {
        roomState.value = InterviewRoomState.FINISHED
        finishRoomEvent.value = event.payload.optString("conversationId").takeIf { it.isNotEmpty() } ?: conversationId
    }

    private fun appendAiDelta(text: String) {
        if (text.isEmpty()) return
        val targetId = currentAiMessageId
        val old = targetId?.let { messageStore[it] }
        if (old != null) {
            ingestMessages(listOf(old.copy(content = old.content + text)))
        } else {
            val item = InterviewRoomMessage(UUID.randomUUID().toString(), InterviewRoomMessage.Role.AI, text)
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
            appendMessage(InterviewRoomMessage.Role.AI, text)
        }
    }

    private fun appendUserMessage(text: String) = appendMessage(InterviewRoomMessage.Role.USER, text)

    private fun showToast(text: String) {
        if (text.isNotEmpty()) {
            toastMessage.value = text
        }
    }

    private fun appendMessage(role: InterviewRoomMessage.Role, text: String) {
        if (text.isEmpty()) return
        ingestMessages(listOf(InterviewRoomMessage(UUID.randomUUID().toString(), role, text)))
    }

    /** 统一合并并刷新消息列表：同 id 覆盖，保证流式内容更新时不重复插入。 */
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

    private fun JsonObject?.optString(name: String): String {
        return runCatching {
            this?.get(name)
                ?.takeIf { !it.isJsonNull && it.isJsonPrimitive }
                ?.asString
                ?.trim()
                .orEmpty()
        }.getOrDefault("")
    }

    companion object {
        private const val ACTION_ENTER_ROOM = "ENTER_ROOM"
        private const val ACTION_TARGET_JOB_UPLOAD = "TARGET_JOB_UPLOAD"
        private const val ACTION_ANSWER_START = "ANSWER_START"
        private const val ACTION_ANSWER_SUBMIT = "ANSWER_SUBMIT"
        private const val ACTION_SESSION_EXIT = "SESSION_EXIT"
        private const val ANSWER_END_USER_SUBMIT = "USER_SUBMIT"
        private const val ANSWER_START_TEXT = "开始作答"
        private const val ANSWER_END_TEXT = "点击结束回答"
    }
}
