package com.shiyuan.fastclaw.core.network.sse.processor

import android.util.Log
import com.google.gson.Gson
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentMessage
import com.shiyuan.fastclaw.core.network.sse.SseChunkProcessor

/**
 * 求职 Agent SSE 解析器：按 SSE event 名分发。
 *
 * 事件协议见《APP消息对接文档》第 4 节：meta / message / snapshot / done / ping，
 * 以及预留的 delta/replace/patch/status/usage/error（当前忽略并记录日志）。
 *
 * 注意：message/snapshot 的 data 是「完整消息对象」（非 token 增量），字段对齐 [JobAgentMessage]，
 * 端侧按 messageId upsert，而不是文本累加。
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
class ClawAgentSseProcessor(
    private val onStart: () -> Unit = {},
    private val onMeta: (requestId: String?, seq: Long?) -> Unit = { _, _ -> },
    private val onMessage: (JobAgentMessage) -> Unit = {},
    /** patch 事件回调，目前用于处理服务端要求端侧实时移除消息。 */
    private val onPatch: (SsePatch) -> Unit = {},
    private val onDone: (status: String?, seq: Long?) -> Unit = { _, _ -> },
    private val onComplete: () -> Unit = {},
    private val onErrorCallback: (Throwable?) -> Unit = {},
) : SseChunkProcessor {

    override fun onSSEStart() = onStart()

    /** 无事件名（裸 data）场景兜底：按 message 解析 */
    override fun onSSEChunk(chunk: String) = onSSEEvent(null, null, chunk)

    override fun onSSEEvent(type: String?, id: String?, data: String) {
        when (type?.trim()?.lowercase()) {
            // event:meta
            EVENT_META -> parse(data, SseMeta::class.java) { onMeta(it.requestId, it.seq) }
            // event:message
            EVENT_MESSAGE, EVENT_SNAPSHOT, null -> parse(data, JobAgentMessage::class.java) { onMessage(it) }
            // event:patch，服务端用来通知端侧删除旧答案气泡或旧下游问题卡。
            EVENT_PATCH -> parse(data, SsePatch::class.java) { onPatch(it) }
            // event:done
            EVENT_DONE -> parse(data, SseDone::class.java) { onDone(it.status, it.seq) }
            // event:ping
            EVENT_PING -> Unit // 心跳保活，不渲染
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
        /** SUCCESS / FAILED / CANCELED */
        val status: String? = null,
        val messageId: String? = null,
    )

    /** patch 事件 data，当前只消费 patchType=REMOVE 的删除指令。 */
    data class SsePatch(
        val requestId: String? = null,
        val seq: Long? = null,
        /** 需要从当前内存列表移除的服务端消息 ID。 */
        val messageId: String? = null,
        /** REMOVE 表示删除当前内存列表中的对应消息。 */
        val patchType: String? = null,
        /** 服务端可见状态，visibleStatus=0 表示该消息已被标记不可见。 */
        val visibleStatus: Int? = null,
        /** 删除原因，仅用于调试排查，端侧展示逻辑不依赖它。 */
        val reason: String? = null,
    )

    companion object {
        private const val TAG = "[SSE-JobAgent]"
        private const val EVENT_META = "meta"
        private const val EVENT_MESSAGE = "message"
        private const val EVENT_SNAPSHOT = "snapshot"
        private const val EVENT_PATCH = "patch"
        private const val EVENT_DONE = "done"
        private const val EVENT_PING = "ping"
        private val gson = Gson()
    }
}
