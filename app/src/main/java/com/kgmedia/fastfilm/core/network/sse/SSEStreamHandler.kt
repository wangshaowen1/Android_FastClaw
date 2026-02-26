package com.kgmedia.fastfilm.core.network.sse

import android.util.Log
import com.kgmedia.fastfilm.core.db.entity.SSEMessage
import com.kgmedia.fastfilm.core.network.SSEvent
import com.kgmedia.fastfilm.core.network.apis.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SSEStreamHandler(
    private val scope: CoroutineScope,
    private val processor: SseChunkProcessor
) {

    private var job: Job? = null

    // 新增：防止重复调用 onComplete
    private var isCompleted = false
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    fun start(flow: Flow<SSEvent>) {
        cancel()
        isCompleted = false // 重置标志位

        _isStreaming.value = true

        job = scope.launch {
            try {
                flow.collect { event ->
                    // 如果已经完成就不再处理后续消息（防御性编程）
                    if (isCompleted) return@collect

                    when (event) {
                        is SSEvent.Open -> {
                            processor.onSSEStart()
                        }

                        is SSEvent.Message -> {
                            val data = event.data
                            try {
                                val sseMessage = Json.decodeFromString<SSEMessage>(data)
                                if (sseMessage.code == SSEMessage.TYPE_ERROR) {
                                    Log.e("[SSE]", "receive msg: parsing error for ${sseMessage.content ?: ""}")
                                    processor.onSSEError(ApiException(-1, sseMessage.content))
                                } else {
                                    if (sseMessage.isOver()) {
                                        handleComplete()
                                        return@collect
                                    }
                                    processor.onSSEChunk(sseMessage.content ?: "")
                                }
                            } catch (e: Exception) {
                                Log.e("[SSE]", "receive msg: parsing error for $data", e)
                                processor.onSSEError(e)
                            }
                        }

                        is SSEvent.Error -> {
                            processor.onSSEError(event.throwable)
                        }

                        is SSEvent.Close -> {
                            handleComplete()
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 正常取消，不作为错误处理
            } catch (e: Exception) {
                processor.onSSEError(e)
            } finally {
                // 确保流自然结束时（非 Close 事件触发）也能回调 complete
                if (isActive && !isCompleted) {
                    handleComplete()
                }
                _isStreaming.value = false
            }
        }
    }

    private fun handleComplete() {
        if (!isCompleted) {
            isCompleted = true
            _isStreaming.value = false
            processor.onSSEComplete()
        }
    }

    fun cancel() {
        _isStreaming.value = false
        job?.cancel()
        job = null
    }
}