package com.kgmedia.fastfilm.core.network.sse.processor

import kotlinx.coroutines.CoroutineScope

/**
 * 通用解析器
 */
class PlainTextSseProcessor(
    scope: CoroutineScope,
    throttleMs: Long = 50L,
    private val onStart: () -> Unit,
    private val onUpdate: (String) -> Unit,
    private val onComplete: () -> Unit,
    private val onErrorCallback: (Throwable?) -> Unit
) : BaseThrottleSseProcessor(scope, throttleMs) {

    private val buffer = StringBuilder(1024)

    override fun onStartInternal() {
        buffer.setLength(0)
        onStart()
    }

    override fun onChunk(chunk: String) {
        buffer.append(chunk)
        tryEmit()
    }

    override fun buildSnapshot(): String = buffer.toString()

    override fun onEmit(content: String) {
        onUpdate(content)
    }

    override fun onCompleteInternal() {
        onComplete()
    }

    override fun onError(e: Throwable?) {
        onErrorCallback(e)
    }
}



