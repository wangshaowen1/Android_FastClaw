package com.shiyuan.fastclaw.core.network.sse

/**
 * SSE chunk 处理策略（解耦点）
 */
interface SseChunkProcessor {

    /** SSE 开始 */
    fun onSSEStart()

    /** 每一段 SSE data */
    fun onSSEChunk(chunk: String)

    /**
     * 带事件名/ID 的 SSE data 回调。默认忽略事件名、按 [onSSEChunk] 处理，兼容仅消费 data 的旧实现；
     * 需要区分 SSE event（如 meta/message/done）的实现重写此方法。
     */
    fun onSSEEvent(type: String?, id: String?, data: String) = onSSEChunk(data)

    /** SSE 正常结束 */
    fun onSSEComplete()

    /** SSE 发生错误 */
    fun onSSEError(e: Throwable?)
}
