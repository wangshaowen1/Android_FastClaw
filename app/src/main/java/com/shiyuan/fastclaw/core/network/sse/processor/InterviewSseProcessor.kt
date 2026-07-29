package com.shiyuan.fastclaw.core.network.sse.processor

import android.util.Log
import com.google.gson.Gson
import com.shiyuan.fastclaw.core.network.apis.response.InterviewAgentMessage
import com.shiyuan.fastclaw.core.network.sse.SseChunkProcessor

/**
 * 模拟面试 SSE 解析器，按后端返回的 SSE event 名称分发。
 *
 * 模拟面试文档 7.2 使用 ROOM_STATE_CHANGED / TEXT_DELTA / TEXT_DONE 等业务事件名，
 * data 承载完整业务消息，解析后统一回调 [InterviewAgentMessage]。
 */
class InterviewSseProcessor(
    /** SSE 连接开始回调。 */
    private val onStart: () -> Unit = {},
    /** meta 事件回调，用于透出 requestId 和服务端序号。 */
    private val onMeta: (requestId: String?, seq: Long?) -> Unit = { _, _ -> },
    /** 业务消息回调，房间状态、文本增量、结束和错误都走这里。 */
    private val onMessage: (InterviewAgentMessage) -> Unit = {},
    /** done 事件回调，用于标记一次 SSE 流结束状态。 */
    private val onDone: (status: String?, seq: Long?) -> Unit = { _, _ -> },
    /** SSE 正常完成回调。 */
    private val onComplete: () -> Unit = {},
    /** SSE 异常回调。 */
    private val onErrorCallback: (Throwable?) -> Unit = {},
) : SseChunkProcessor {

    /** 建连成功后通知上层更新连接状态。 */
    override fun onSSEStart() = onStart()

    /** 无 event 名兜底按业务消息解析，兼容只透传 data 的 SSE 流。 */
    override fun onSSEChunk(chunk: String) = onSSEEvent(null, null, chunk)

    override fun onSSEEvent(type: String?, id: String?, data: String) {
        // 后端 eventType 按文档只需要转小写匹配，避免大小写差异导致漏处理。
        when (type?.trim()?.lowercase()) {
            // meta 只更新流元信息，不进入消息列表。
            EVENT_META -> parse(data, SseMeta::class.java) { onMeta(it.requestId, it.seq) }
            // 这些业务事件的 data 都是 InterviewAgentMessage，交给 ViewModel 统一处理。
            EVENT_ROOM_STATE_CHANGED,
            EVENT_TEXT_DELTA,
            EVENT_TEXT_DONE,
            EVENT_TEXT_BLOCKED,
            EVENT_INTERVIEW_FINISHED,
            EVENT_ERROR,
            null -> parse(data, InterviewAgentMessage::class.java) { onMessage(it) }
            // done 表示当前 SSE 流结束，不等同于面试业务结束。
            EVENT_DONE -> parse(data, SseDone::class.java) { onDone(it.status, it.seq) }
            // 心跳事件只用于保活，不需要通知业务层。
            EVENT_PING, EVENT_HEARTBEAT -> Unit
            else -> Log.d(TAG, "ignore reserved sse event: type=$type, data=$data")
        }
    }

    override fun onSSEComplete() = onComplete()

    override fun onSSEError(e: Throwable?) = onErrorCallback(e)

    private fun <T> parse(data: String, clazz: Class<T>, block: (T) -> Unit) {
        // 单条事件解析失败只打印日志，不中断后续 SSE 消费。
        runCatching { gson.fromJson(data, clazz) }
            .onSuccess { it?.let(block) }
            .onFailure { Log.e(TAG, "sse parse fail for ${clazz.simpleName}: $data", it) }
    }

    /** meta 事件 data。 */
    data class SseMeta(
        val requestId: String? = null,
        val seq: Long? = null,
    )

    /** done 事件 data。 */
    data class SseDone(
        val requestId: String? = null,
        val seq: Long? = null,
        val status: String? = null,
        val messageId: String? = null,
    )

    companion object {
        private const val TAG = "[SSE-Interview]"
        /** 元信息事件。 */
        private const val EVENT_META = "meta"
        /** 预留 message 事件名，目前业务事件按具体类型分发。 */
        private const val EVENT_MESSAGE = "message"
        /** 预留快照事件名。 */
        private const val EVENT_SNAPSHOT = "snapshot"
        /** SSE 流结束事件。 */
        private const val EVENT_DONE = "done"
        /** ping 心跳事件。 */
        private const val EVENT_PING = "ping"
        /** heartbeat 心跳事件。 */
        private const val EVENT_HEARTBEAT = "heartbeat"
        /** 房间状态变化事件。 */
        private const val EVENT_ROOM_STATE_CHANGED = "room_state_changed"
        /** 文本流式增量事件。 */
        private const val EVENT_TEXT_DELTA = "text_delta"
        /** 文本流式完成事件。 */
        private const val EVENT_TEXT_DONE = "text_done"
        /** 用户回答违规替换事件。 */
        private const val EVENT_TEXT_BLOCKED = "text_blocked"
        /** 面试业务结束事件。 */
        private const val EVENT_INTERVIEW_FINISHED = "interview_finished"
        /** 业务错误事件。 */
        private const val EVENT_ERROR = "error"
        private val gson = Gson()
    }
}
