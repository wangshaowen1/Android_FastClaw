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

    fun start(flow: Flow<SSEvent>) {
        cancel()

        job = scope.launch {
            try {
                flow.collect { event ->
                    when (event) {
                        is SSEvent.Open -> {
                            processor.onStart()
                        }
                        is SSEvent.Message -> {
                            // 将原始的 data 丢给 processor
                            // processor 内部会处理 Json 解析和业务逻辑
                            processor.onChunk(event.data)
                        }
                        is SSEvent.Error -> {
                            processor.onError(event.throwable)
                        }
                        is SSEvent.Close -> {
                            processor.onComplete()
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 正常取消
            } catch (e: Exception) {
                processor.onError(e)
            } finally {
                // 确保流结束时回调 complete
                if (isActive) {
                    processor.onComplete()
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
