package com.kgmedia.fastfilm.core.network.sse.processor

import com.kgmedia.fastfilm.core.network.sse.SseChunkProcessor
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 故事梗概解析器
 */
class EpisodeSseProcessor(
    private val scope: CoroutineScope,
    private val onTitleUpdate: (String) -> Unit,
    private val onMdUpdate: (String) -> Unit,
    private val onStart: () -> Unit,
    private val onComplete: (String) -> Unit,
    private val onErrorCallback: (Throwable?) -> Unit
) : SseChunkProcessor {
    private val fullSourceAccumulator = StringBuilder()
    private val parser = JsonSseFieldStreamer(
        onTitleUpdate = { title ->
            scope.launch(Dispatchers.Main) {
                onTitleUpdate(title)
            }
        },
        onMdUpdate = { md ->
            scope.launch(Dispatchers.Main) {
                onMdUpdate(md)
            }
        }
    )

    override fun onSSEStart() {
        fullSourceAccumulator.setLength(0)
        parser.reset()
        scope.launch(Dispatchers.Main) { onStart() }
    }

    override fun onSSEChunk(chunk: String) {
        Logger.d("[SSE]", "onSSEChunk $chunk")
        fullSourceAccumulator.append(chunk)
        parser.onChunk(chunk)
    }

    override fun onSSEComplete() {
        val fullSource = fullSourceAccumulator.toString()
        scope.launch(Dispatchers.Main) {
            // 4. 传给回调
            onComplete(fullSource)
        }
    }

    override fun onSSEError(e: Throwable?) {
        scope.launch(Dispatchers.Main) { onErrorCallback(e) }
    }
}

