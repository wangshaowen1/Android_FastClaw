package com.shiyuan.fastclaw.core.network.sse.processor

import android.util.Log
import com.google.gson.Gson
import com.shiyuan.fastclaw.core.network.apis.response.InterviewAgentMessage
import com.shiyuan.fastclaw.core.network.sse.SseChunkProcessor

/**
 * 模拟面试 SSE 解析器：按后端返回的 SSE event 名称分发。
 *
 * 协议保持和求职 Agent 一致：meta / message / snapshot / done / ping。
 * message 和 snapshot 承载完整业务消息，直接回调 [InterviewAgentMessage]。
 */
class InterviewSseProcessor(
    private val onStart: () -> Unit = {},
    private val onMeta: (requestId: String?, seq: Long?) -> Unit = { _, _ -> },
    private val onMessage: (InterviewAgentMessage) -> Unit = {},
    private val onDone: (status: String?, seq: Long?) -> Unit = { _, _ -> },
    private val onComplete: () -> Unit = {},
    private val onErrorCallback: (Throwable?) -> Unit = {},
) : SseChunkProcessor {

    override fun onSSEStart() = onStart()

    /** 无 event 名兜底按 message 解析，兼容只透传 data 的流。 */
    override fun onSSEChunk(chunk: String) = onSSEEvent(null, null, chunk)

    override fun onSSEEvent(type: String?, id: String?, data: String) {
        when (type?.trim()?.lowercase()) {
            EVENT_META -> parse(data, SseMeta::class.java) { onMeta(it.requestId, it.seq) }
            EVENT_MESSAGE, EVENT_SNAPSHOT, null -> parse(data, InterviewAgentMessage::class.java) { onMessage(it) }
            EVENT_DONE -> parse(data, SseDone::class.java) { onDone(it.status, it.seq) }
            EVENT_PING, EVENT_HEARTBEAT -> Unit
            EVENT_ERROR -> parse(data, SseError::class.java) {
                onErrorCallback(IllegalStateException(it.errorMessage ?: it.message ?: data))
            }
            else -> Log.d(TAG, "ignore reserved sse event: type=$type, data=$data")
        }
    }

    override fun onSSEComplete() = onComplete()

    override fun onSSEError(e: Throwable?) = onErrorCallback(e)

    private fun <T> parse(data: String, clazz: Class<T>, block: (T) -> Unit) {
        runCatching { gson.fromJson(data, clazz) }
            .onSuccess { it?.let(block) }
            .onFailure { Log.e(TAG, "sse parse fail for ${clazz.simpleName}: $data", it) }
    }

    /** meta 事件 data */
    data class SseMeta(
        val requestId: String? = null,
        val seq: Long? = null,
    )

    /** done 事件 data */
    data class SseDone(
        val requestId: String? = null,
        val seq: Long? = null,
        val status: String? = null,
        val messageId: String? = null,
    )

    /** error 事件 data */
    data class SseError(
        val message: String? = null,
        val errorMessage: String? = null,
    )

    companion object {
        private const val TAG = "[SSE-Interview]"
        private const val EVENT_META = "meta"
        private const val EVENT_MESSAGE = "message"
        private const val EVENT_SNAPSHOT = "snapshot"
        private const val EVENT_DONE = "done"
        private const val EVENT_PING = "ping"
        private const val EVENT_HEARTBEAT = "heartbeat"
        private const val EVENT_ERROR = "error"
        private val gson = Gson()
    }
}
