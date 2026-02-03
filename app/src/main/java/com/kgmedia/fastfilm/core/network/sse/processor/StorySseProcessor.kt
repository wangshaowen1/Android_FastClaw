package com.kgmedia.fastfilm.core.network.sse.processor

import com.kgmedia.fastfilm.core.network.sse.SseChunkProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 故事内容解析器
 */
class StorySseProcessor(
    private val scope: CoroutineScope,
    private val onStoryUpdate: (String) -> Unit, // 只关注 story 内容
    private val onStart: () -> Unit,
    private val onComplete: (String) -> Unit,    // 完成时返回完整 JSON
    private val onErrorCallback: (Throwable?) -> Unit
) : SseChunkProcessor {

    private val fullSourceAccumulator = StringBuilder()

    private val parser = JsonSseFieldStreamer(
        onStoryUpdate = { content ->
            scope.launch(Dispatchers.Main) {
                onStoryUpdate(content)
            }
        }
    )

    override fun onStart() {
        fullSourceAccumulator.setLength(0)
        scope.launch(Dispatchers.Main) { onStart() }
    }

    override fun onChunk(chunk: String) {
        fullSourceAccumulator.append(chunk) // 记录原始数据
        parser.onChunk(chunk)               // 解析字段
    }

    override fun onComplete() {
        val fullSource = fullSourceAccumulator.toString()
        scope.launch(Dispatchers.Main) { onComplete(fullSource) }
    }

    override fun onError(e: Throwable?) {
        scope.launch(Dispatchers.Main) { onErrorCallback(e) }
    }
}