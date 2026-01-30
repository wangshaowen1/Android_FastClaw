package com.kgmedia.fastfilm.core.network

import android.os.SystemClock
import com.kgmedia.fastfilm.core.db.entity.SSEMessage
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SSEStreamHandler(
    private val scope: CoroutineScope, // 传入ViewModelScope
    private val onStart: () -> Unit,
    private val onContentUpdate: (String) -> Unit,
    private val onComplete: () -> Unit,
    private val onError: (Throwable?) -> Unit
) {

    private var sseJob: Job? = null
    private val sseBuffer = StringBuilder()
    private var lastUiEmitTime = 0L

    companion object {
        const val UI_EMIT_INTERVAL = 50L
    }

    /**
     * 开始监听SSE流
     * @param sseFlow API返回的Flow对象
     */
    fun start(sseFlow: Flow<SSEvent>) {
        cancel() // 确保之前的Job已清除
        sseJob = scope.launch {
            try {
                sseFlow.collect { event ->
                    handleEvent(event)
                }
            } catch (e: CancellationException) {
                // 协程被取消，不做额外处理
                throw e
            } catch (e: Exception) {
                onError(e)
            } finally {
                // 流结束或异常时的清理工作，可以在这里做，也可以依赖外部调用
            }
        }
    }

    private fun handleEvent(event: SSEvent) {
        when (event) {
            is SSEvent.Open -> {
                sseBuffer.clear()
                lastUiEmitTime = 0L
                onStart()
            }

            is SSEvent.Message -> {
                try {
                    val data = event.data
                    // 假设 SSEMessage 是通用的解析对象
                    val sseMessage = Json.decodeFromString<SSEMessage>(data)

                    if (sseMessage.code == SSEMessage.TYPE_ERROR) {
//                        sseMessage.content?.showToast() // 这里最好把Toast回调出去，保持纯净，暂时保留你的逻辑
                        onError(RuntimeException(sseMessage.content))
                    } else {
                        processMessage(sseMessage)
                    }
                } catch (e: Exception) {
                    onError(e)
                }
            }

            is SSEvent.Error -> onError(event.throwable)
            is SSEvent.Close -> cancel() // 或者调用 onComplete()，视服务端协议而定
        }
    }

    private fun processMessage(sseMessage: SSEMessage) {
        // 1. 判断是否结束
        if (sseMessage.isOver()) {
            emitToUi(force = true) // 强制刷新最后一次UI
            sseBuffer.clear()
            onComplete()
            return
        }

        // 2. 追加内容并节流刷新
        sseMessage.content?.let { content ->
            sseBuffer.append(content)
            emitToUi(force = false)
        }
    }

    private fun emitToUi(force: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastUiEmitTime < UI_EMIT_INTERVAL) return

        lastUiEmitTime = now
        // 回调当前完整的Buffer内容
        onContentUpdate(sseBuffer.toString())
    }

    /**
     * 外部手动停止/清理
     */
    fun cancel() {
        sseJob?.cancel()
        sseJob = null
    }
}