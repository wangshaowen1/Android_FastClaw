package com.kgmedia.fastfilm.core.network.sse

import com.kgmedia.fastfilm.core.network.SSEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SSEStreamHandler(
    private val scope: CoroutineScope,
    private val processor: SseChunkProcessor
) {

    private var job: Job? = null

    // 新增：防止重复调用 onComplete
    private var isCompleted = false

    fun start(flow: Flow<SSEvent>) {
        cancel()
        isCompleted = false // 重置标志位

        job = scope.launch {
            try {
                flow.collect { event ->
                    // 如果已经完成就不再处理后续消息（防御性编程）
                    if (isCompleted) return@collect

                    when (event) {
                        is SSEvent.Open -> {
                            processor.onStart()
                        }

                        is SSEvent.Message -> {
                            processor.onChunk(event.data)
                        }

                        is SSEvent.Error -> {
                            processor.onError(event.throwable)
                        }

                        is SSEvent.Close -> {
                            handleComplete()
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 正常取消，不作为错误处理
            } catch (e: Exception) {
                processor.onError(e)
            } finally {
                // 确保流自然结束时（非 Close 事件触发）也能回调 complete
                if (isActive && !isCompleted) {
                    handleComplete()
                }
            }
        }
    }

    private fun handleComplete() {
        if (!isCompleted) {
            isCompleted = true
            processor.onComplete()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}