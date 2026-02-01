package com.kgmedia.fastfilm.core.network.sse

/**
 * SSE chunk 处理策略（解耦点）
 */
interface SseChunkProcessor {

    /** SSE 开始 */
    fun onStart()

    /** 每一段 SSE data */
    fun onChunk(chunk: String)

    /** SSE 正常结束 */
    fun onComplete()

    /** SSE 发生错误 */
    fun onError(e: Throwable?)
}
