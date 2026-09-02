package com.shiyuan.fastclaw.feature.clawagent.message

import android.util.Log
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.core.network.apis.response.ActivityPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.AttributeConfirmPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentMessage
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewEntryPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import java.util.UUID
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentUiEffect

/**
 * Agent 消息的内存去重、乐观消息合并、服务端模型映射与卡片轮询识别。
 * 不请求接口，只通过回调把渲染列表、一次性效果和轮询意图交还 ViewModel。
 */
internal class ClawAgentMessageStore(
    private val onMessagesChanged: (List<ClawAgentMessageInfo>) -> Unit,
    private val onUiEffect: (ClawAgentUiEffect) -> Unit,
    private val onMockInterviewReportPolling: (mockReportId: String?, interviewId: String?) -> Unit,
    private val onPassPackageReportPolling: (packageId: String?) -> Unit,
) {

    private val messages = LinkedHashMap<String, JobAgentMessage>()
    private val optimisticUserMessageIds = LinkedHashSet<String>()
    private val serverUserMessageKeyToLocalKey = HashMap<String, String>()
    private var lastPublishedMessages: List<ClawAgentMessageInfo>? = null

    fun lastCursor(): String? = messages.entries.lastOrNull()?.value?.cursor

    fun oldestCursor(): String? =
        messages.values.minByOrNull { it.sequenceNo ?: Long.MAX_VALUE }?.cursor

    fun clear() {
        messages.clear()
        lastPublishedMessages = emptyList()
    }

    /** 仅替换端侧临时卡列表，不写入服务端消息缓存。 */
    fun showLocalMessages(items: List<ClawAgentMessageInfo>) {
        lastPublishedMessages = items
        onMessagesChanged(items)
    }

    fun removeLastThinkMessage() {
        val lastEntry = messages.entries.lastOrNull()
        val lastMessage = lastEntry?.value
        if (lastMessage != null && resolveItemType(lastMessage) == ClawAgentMsgAdapter.TYPE_AGENT_THINK) {
            messages.remove(lastEntry.key)
        }
    }

    fun appendLocalUserMessage(clientMessageId: String, content: String) {
        val nextSequence = (messages.values.mapNotNull { it.sequenceNo }.maxOrNull() ?: 0L) + 1
        optimisticUserMessageIds.add(clientMessageId)
        ingest(
            listOf(
                JobAgentMessage(
                    messageId = clientMessageId,
                    sequenceNo = nextSequence,
                    senderType = SENDER_USER,
                    messageType = MSG_TEXT,
                    content = content,
                )
            )
        )
    }

    fun insertSseMessage(newMessage: JobAgentMessage?) {
        newMessage ?: return
        val lastEntry = messages.entries.lastOrNull()
        val lastMessage = lastEntry?.value
        if (
            lastMessage != null &&
            lastMessage.messageId != newMessage.messageId &&
            resolveItemType(lastMessage) == ClawAgentMsgAdapter.TYPE_AGENT_THINK
        ) {
            when (lastMessage.displayPolicy) {
                POLICY_REPLACE_ON_TERMINAL,
                POLICY_DISMISS_ON_TERMINAL -> messages.remove(lastEntry.key)
                else -> Unit
            }
        }
        if (newMessage.isAttributeConfirm()) {
            val payload = newMessage.payloadJson?.let {
                runCatching {
                    GsonUtils.fromJson(it, AttributeConfirmPayloadBean::class.java)
                }.getOrNull()
            }
            if (payload?.interactionState?.disabled == false) {
                onUiEffect(ClawAgentUiEffect.ShowMemoryConfirm(newMessage.toMessageInfo()))
            }
        }
        when {
            newMessage.isNotificationSwitch() -> {
                onUiEffect(ClawAgentUiEffect.ShowNotificationSwitch)
            }

            newMessage.isUnderstandingSummaryCard() -> {
                onUiEffect(ClawAgentUiEffect.ShowHelpYouFindJob)
            }
        }
        ingest(listOf(newMessage))
    }

    fun ingest(newMessages: List<JobAgentMessage>?) {
        if (newMessages.isNullOrEmpty()) return
        newMessages.forEach { newMessage ->
            if (newMessage.isAttributeConfirm() || newMessage.isNotificationSwitch()) {
                return@forEach
            }
            val rawKey = newMessage.messageId
                ?: newMessage.cursor
                ?: newMessage.sequenceNo?.toString()
                ?: return@forEach

            if (newMessage.isIntroduceYourselfCard()) {
                messages.filterValues { it.isIntroduceYourselfCard() }
                    .keys
                    .filter { it != rawKey }
                    .forEach { messages.remove(it) }
            }

            var key = rawKey
            if (newMessage.senderType == SENDER_USER) {
                key = serverUserMessageKeyToLocalKey[rawKey] ?: rawKey
                if (key != rawKey) {
                    messages.remove(rawKey)
                    messages[key] = newMessage.copy(messageId = key)
                    newMessage.rememberServerKeys(key)
                    return@forEach
                }
            }

            if (newMessage.senderType == SENDER_USER && key !in optimisticUserMessageIds) {
                val localKey = optimisticUserMessageIds.firstOrNull {
                    messages[it]?.content == newMessage.content
                }
                if (localKey != null) {
                    messages.remove(rawKey)
                    messages[localKey] = newMessage.copy(messageId = localKey)
                    optimisticUserMessageIds.remove(localKey)
                    newMessage.rememberServerKeys(localKey)
                    return@forEach
                }
            }

            newMessage.responseGroupId?.takeIf { it.isNotBlank() }?.let { groupId ->
                messages.filterValues {
                    it.responseGroupId == groupId &&
                        (it.displayPolicy == POLICY_REPLACE_ON_TERMINAL ||
                            it.displayPolicy == POLICY_DISMISS_ON_TERMINAL)
                }
                    .keys
                    .filter { it != key }
                    .forEach { messages.remove(it) }
            }

            if (!newMessage.isToastCard()) {
                messages[key] = newMessage
            }
        }
        startPollingForVisibleCards()
        publishMessages()
    }

    fun removeById(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        val localKey = serverUserMessageKeyToLocalKey[messageId]
        val keysToRemove = messages.filter { (key, message) ->
            key == messageId ||
                key == localKey ||
                message.messageId == messageId ||
                message.cursor == messageId ||
                message.sequenceNo?.toString() == messageId
        }.keys.toMutableSet()
        localKey?.let(keysToRemove::add)
        if (keysToRemove.isEmpty()) return
        keysToRemove.forEach { key ->
            messages.remove(key)
            optimisticUserMessageIds.remove(key)
        }
        serverUserMessageKeyToLocalKey.entries.removeAll { (serverKey, mappedLocalKey) ->
            serverKey == messageId || serverKey in keysToRemove || mappedLocalKey in keysToRemove
        }
        Log.d("sseProcessor_onPatch=", messageId)
        publishMessages()
    }

    fun expandIntroduceBeforeUploadResume(
        newMessages: List<JobAgentMessage>?,
        shouldInsertIntroduce: Boolean,
    ): List<JobAgentMessage> {
        if (!shouldInsertIntroduce) return newMessages.orEmpty()
        return listOf(
            JobAgentMessage(
                messageId = LOCAL_INTRODUCE_MESSAGE_ID,
                senderType = SENDER_AGENT,
                messageType = MSG_CARD,
                cardType = CARD_INTRODUCE_YOURSELF,
                sequenceNo = 0L,
            )
        ) + newMessages.orEmpty()
    }

    fun isThinkMessage(message: JobAgentMessage): Boolean =
        resolveItemType(message) == ClawAgentMsgAdapter.TYPE_AGENT_THINK

    fun shouldSkipPullAfterStreamComplete(message: JobAgentMessage): Boolean =
        message.messageType == MSG_CARD && message.cardType == CARD_UNDERSTANDING_SUMMARY

    private fun startPollingForVisibleCards() {
        messages.values.forEach { message ->
            if (message.messageType == MSG_CARD && message.cardType == CARD_MOCK_INTERVIEW_REPORT) {
                val payload = message.payloadJson?.let {
                    runCatching {
                        GsonUtils.fromJson(it, MockInterviewReportPayloadBean::class.java)
                    }.getOrNull()
                }
                if (payload?.reportStatus == REPORT_STATUS_GENERATING) {
                    onMockInterviewReportPolling(payload.mockReportId, payload.interviewId)
                }
            }
            if (message.messageType == MSG_CARD && message.cardType == CARD_INTERVIEW_PASS_PACKAGE_REPORT) {
                val payload = message.payloadJson?.let {
                    runCatching {
                        GsonUtils.fromJson(it, PassPackageReportPayloadBean::class.java)
                    }.getOrNull()
                }
                if (payload?.reportStatus == REPORT_STATUS_GENERATING) {
                    onPassPackageReportPolling(payload.packageId)
                }
            }
        }
    }

    private fun publishMessages() {
        val rawMessages = messages.values.sortedBy { it.sequenceNo ?: Long.MAX_VALUE }
        val renderMessages = rawMessages.map { it.toMessageInfo() }
        if (lastPublishedMessages == renderMessages) return
        lastPublishedMessages = renderMessages
        onMessagesChanged(renderMessages)
        if (BuildConfig.DEBUG) {
            println("messageList=---------------------------------------------------------")
            rawMessages.forEachIndexed { index, message ->
                println("messageList[$index]=${GsonUtils.toJson(message)}")
            }
        }
    }

    private fun JobAgentMessage.rememberServerKeys(localKey: String) {
        listOfNotNull(messageId, cursor, sequenceNo?.toString())
            .filter { it != localKey }
            .forEach { serverUserMessageKeyToLocalKey[it] = localKey }
    }

    private fun JobAgentMessage.toMessageInfo(): ClawAgentMessageInfo {
        val sender = senderType.orEmpty()
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

    private fun resolveItemType(message: JobAgentMessage): Int {
        if (message.senderType == SENDER_USER) {
            return if (message.messageType == MSG_CARD && message.cardType == CARD_FILE_CARD) {
                ClawAgentMsgAdapter.TYPE_USER_FILE
            } else {
                ClawAgentMsgAdapter.TYPE_USER_TEXT
            }
        }
        if (
            message.messageType != MSG_CARD &&
            (message.messagePhase == PHASE_PROCESSING || message.messageStatus == STATUS_STREAMING)
        ) {
            return ClawAgentMsgAdapter.TYPE_AGENT_THINK
        }
        if (message.messageType == MSG_TEXT_TIP) return ClawAgentMsgAdapter.TYPE_TEXT_TIP
        if (message.messageType != MSG_CARD) return ClawAgentMsgAdapter.TYPE_AGENT_TEXT
        return when (message.cardType) {
            CARD_INTRODUCE_YOURSELF -> ClawAgentMsgAdapter.TYPE_WELCOME
            CARD_ACTIVITY -> resolveActivityItemType(message)
            CARD_UPLOAD_RESUME -> resolveUploadResumeItemType(message)
            CARD_HISTORY_RESUME_CONFIRM -> ClawAgentMsgAdapter.TYPE_PENDING_CONFIRM
            CARD_CORE_QUESTION -> resolveQuestionItemType(message)
            CARD_UNDERSTANDING_SUMMARY -> ClawAgentMsgAdapter.TYPE_UNDERSTANDING_UPGRADE
            CARD_RESUME_RECEIVED -> ClawAgentMsgAdapter.TYPE_UPLOAD_SUCCESS
            CARD_JOB_SEARCHING -> ClawAgentMsgAdapter.TYPE_SEARCHING_JOBS
            CARD_NO_JOB -> ClawAgentMsgAdapter.TYPE_NO_JOBS_FOUND
            CARD_COMPANY_BACKGROUND_ENTRY -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_ENTRY
            CARD_COMPANY_BACKGROUND_REPORT -> resolveCompanyBackgroundReportItemType(message)
            CARD_MOCK_INTERVIEW_ENTRY -> resolveMockInterviewEntryItemType(message)
            CARD_MOCK_INTERVIEW_REPORT -> resolveMockInterviewReportItemType(message)
            CARD_INTERVIEW_PASS_PACKAGE_REPORT -> resolvePassPackageReportItemType(message)
            else -> ClawAgentMsgAdapter.TYPE_AGENT_TEXT
        }
    }

    private fun resolveUploadResumeItemType(message: JobAgentMessage): Int {
        val status = message.payloadJson?.let {
            runCatching {
                GsonUtils.fromJson(it, UploadResumePayloadBean::class.java)?.resumeUploadStatus
            }.getOrNull()
        }
        return when (status) {
            UPLOAD_STATUS_PARSE_FAILED -> ClawAgentMsgAdapter.TYPE_UPLOAD_FAILED
            else -> ClawAgentMsgAdapter.TYPE_UPLOAD_WAIT
        }
    }

    private fun resolveActivityItemType(message: JobAgentMessage): Int {
        val activityType = message.payloadJson?.let {
            runCatching {
                GsonUtils.fromJson(it, ActivityPayloadBean::class.java)?.activityType
            }.getOrNull()
        }
        return if (activityType == ActivityPayloadBean.TYPE_SALARY) {
            ClawAgentMsgAdapter.TYPE_ACTIVITY_SALARY
        } else {
            ClawAgentMsgAdapter.TYPE_AGENT_TEXT
        }
    }

    private fun resolveQuestionItemType(message: JobAgentMessage): Int {
        val inputMode = message.payloadJson?.let {
            runCatching {
                GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java)?.inputMode
            }.getOrNull()
        }
        return if (inputMode == CoreQuestionPayloadBean.MODE_ARRAY_FORM) {
            ClawAgentMsgAdapter.TYPE_TABLE_FORM
        } else {
            ClawAgentMsgAdapter.TYPE_QUESTION
        }
    }

    private fun resolveCompanyBackgroundReportItemType(message: JobAgentMessage): Int {
        val status = message.payloadJson?.let {
            runCatching {
                GsonUtils.fromJson(it, CompanyBackgroundReportPayloadBean::class.java)?.reportStatus
            }.getOrNull()
        }
        return when (status) {
            REPORT_STATUS_FAILED -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_REPORT_FAILED
            REPORT_STATUS_SUCCESS -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_REPORT_DONE
            else -> ClawAgentMsgAdapter.TYPE_COMPANY_BACKGROUND_REPORT_GENERATING
        }
    }

    private fun resolveMockInterviewEntryItemType(message: JobAgentMessage): Int {
        val payload = message.payloadJson?.let {
            runCatching {
                GsonUtils.fromJson(it, MockInterviewEntryPayloadBean::class.java)
            }.getOrNull()
        }
        return if (payload?.entrySource == RESUME_UNDERSTANDING_UPGRADE) {
            ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_ENTRY_UPGRADE
        } else {
            ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_ENTRY
        }
    }

    private fun resolveMockInterviewReportItemType(message: JobAgentMessage): Int {
        val status = message.payloadJson?.let {
            runCatching {
                GsonUtils.fromJson(it, MockInterviewReportPayloadBean::class.java)?.reportStatus
            }.getOrNull()
        }
        return when (status) {
            REPORT_STATUS_FAILED -> ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_REPORT_FAILED
            REPORT_STATUS_SUCCESS -> ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_REPORT_DONE
            else -> ClawAgentMsgAdapter.TYPE_MOCK_INTERVIEW_REPORT_GENERATING
        }
    }

    private fun resolvePassPackageReportItemType(message: JobAgentMessage): Int {
        val status = message.payloadJson?.let {
            runCatching {
                GsonUtils.fromJson(it, PassPackageReportPayloadBean::class.java)?.reportStatus
            }.getOrNull()
        }
        return when (status) {
            REPORT_STATUS_FAILED -> ClawAgentMsgAdapter.TYPE_PASS_PACKAGE_REPORT_FAILED
            REPORT_STATUS_SUCCESS -> ClawAgentMsgAdapter.TYPE_PASS_PACKAGE_REPORT_DONE
            else -> ClawAgentMsgAdapter.TYPE_PASS_PACKAGE_REPORT_GENERATING
        }
    }

    private fun JobAgentMessage.isToastCard(): Boolean = messageType == MSG_TOAST

    private fun JobAgentMessage.isAttributeConfirm(): Boolean =
        messageType == MSG_CARD && cardType == CARD_ATTRIBUTE_CONFIRM

    private fun JobAgentMessage.isNotificationSwitch(): Boolean =
        messageType == MSG_CARD && cardType == CARD_NOTIFICATION_SWITCH

    private fun JobAgentMessage.isIntroduceYourselfCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_INTRODUCE_YOURSELF

    private fun JobAgentMessage.isUnderstandingSummaryCard(): Boolean =
        messageType == MSG_CARD && cardType == CARD_UNDERSTANDING_SUMMARY

    companion object {
        const val SENDER_USER = "USER"
        const val SENDER_AGENT = "AGENT"

        private const val MSG_TEXT = "TEXT"
        private const val MSG_CARD = "CARD"
        private const val MSG_TOAST = "TOAST"
        private const val MSG_TEXT_TIP = "TEXT_TIP"
        private const val PHASE_PROCESSING = "PROCESSING"
        private const val STATUS_STREAMING = "STREAMING"
        private const val POLICY_REPLACE_ON_TERMINAL = "REPLACE_ON_TERMINAL"
        private const val POLICY_DISMISS_ON_TERMINAL = "DISMISS_ON_TERMINAL"
        private const val UPLOAD_STATUS_PARSE_FAILED = "PARSE_FAILED"
        private const val REPORT_STATUS_GENERATING = "GENERATING"
        private const val REPORT_STATUS_SUCCESS = "SUCCESS"
        private const val REPORT_STATUS_FAILED = "FAILED"
        private const val RESUME_UNDERSTANDING_UPGRADE = "RESUME_UNDERSTANDING_UPGRADE"

        private const val CARD_UPLOAD_RESUME = "UPLOAD_RESUME"
        private const val CARD_RESUME_RECEIVED = "RESUME_RECEIVED"
        private const val CARD_HISTORY_RESUME_CONFIRM = "HISTORY_RESUME_CONFIRM"
        private const val CARD_CORE_QUESTION = "CORE_QUESTION"
        private const val CARD_ATTRIBUTE_CONFIRM = "ATTRIBUTE_CONFIRM"
        private const val CARD_NOTIFICATION_SWITCH = "NOTIFICATION_SWITCH"
        private const val CARD_UNDERSTANDING_SUMMARY = "UNDERSTANDING_SUMMARY"
        private const val CARD_INTRODUCE_YOURSELF = "CARD_INTRODUCE_YOURSELF"
        private const val CARD_ACTIVITY = "ACTIVITY"
        private const val CARD_COMPANY_BACKGROUND_ENTRY = "COMPANY_BACKGROUND_ENTRY"
        private const val CARD_COMPANY_BACKGROUND_REPORT = "COMPANY_BACKGROUND_REPORT"
        private const val CARD_MOCK_INTERVIEW_ENTRY = "MOCK_INTERVIEW_ENTRY"
        private const val CARD_MOCK_INTERVIEW_REPORT = "MOCK_INTERVIEW_REPORT"
        private const val CARD_INTERVIEW_PASS_PACKAGE_REPORT = "INTERVIEW_PASS_PACKAGE_REPORT"
        private const val CARD_FILE_CARD = "FILE_CARD"
        private const val CARD_JOB_SEARCHING = "JOB_SEARCHING"
        private const val CARD_NO_JOB = "NO_JOB_TEMPORARILY"
        private const val LOCAL_INTRODUCE_MESSAGE_ID = "-1"
    }
}
