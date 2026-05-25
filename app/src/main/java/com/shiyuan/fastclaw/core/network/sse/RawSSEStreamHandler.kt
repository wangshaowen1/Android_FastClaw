package com.shiyuan.fastclaw.core.network.sse

import android.util.Log
import com.shiyuan.fastclaw.core.network.SSEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 原始 SSE 处理链：不解析 SSEMessage 协议，每条 data 直接透传给 processor.onSSEChunk。
 *
 * 适用于服务端按通用 SSE（如 OpenAI 风格的 chunk、纯文本块）返回的接口。
 */
class RawSSEStreamHandler(
    private val scope: CoroutineScope,
    private val processor: SseChunkProcessor
) {

    private var job: Job? = null

    private val isFinished = AtomicBoolean(false)
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    private var hasTriggeredStart = false

    private fun ensureStartedSafely() {
        if (!hasTriggeredStart) {
            hasTriggeredStart = true
            processor.onSSEStart()
        }
    }

    fun start(flow: Flow<SSEvent>) {
        cancel()
        isFinished.set(false)
        hasTriggeredStart = false
        _isStreaming.value = true
        job = scope.launch(Dispatchers.Main) {
            try {
                flow.collect { event ->
                    if (isFinished.get()) return@collect
                    when (event) {
                        is SSEvent.Open -> {
                            Log.d("[SSE-Raw]", "Open")
                            ensureStartedSafely()
                        }

                        is SSEvent.Message -> {
                            ensureStartedSafely()
                            val data = event.data
                            Log.d("[SSE-Raw]", "Message: type=${event.type}, id=${event.id}, data=$data")
                            // 透传事件名（meta/message/done…），由 processor 按 event 分发
                            processor.onSSEEvent(event.type, event.id, data)
                        }

                        is SSEvent.Error -> {
                            Log.d("[SSE-Raw]", "Error: ${event.throwable}")
                            handleError(event.throwable)
                        }

                        // done 事件若因末尾缺空行未被 okhttp 派发，会以 Close 收尾走到这里
                        is SSEvent.Close -> {
                            Log.d("[SSE-Raw]", "Close: isServiceClose=${event.isServiceClose}")
                            handleComplete()
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.e("[SSE-Raw]", "Stream cancelled normally")
            } catch (e: Exception) {
                handleError(e)
            } finally {
                if (isFinished.compareAndSet(false, true)) {
                    processor.onSSEComplete()
                }
                _isStreaming.value = false
            }
        }
    }

    private fun handleComplete() {
        if (isFinished.compareAndSet(false, true)) {
            _isStreaming.value = false
            processor.onSSEComplete()
        }
    }

    private fun handleError(e: Throwable?) {
        if (isFinished.compareAndSet(false, true)) {
            _isStreaming.value = false
            processor.onSSEError(e)
            job?.cancel()
        }
    }

    fun cancel() {
        isFinished.set(true)
        _isStreaming.value = false
        job?.cancel()
        job = null
    }
}