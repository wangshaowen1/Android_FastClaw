package com.kgmedia.fastfilm.core.network.sse.processor

import com.kgmedia.fastfilm.core.network.sse.SseChunkProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EpisodeSseProcessor(
    private val scope: CoroutineScope,
    private val onTitleUpdate: (String) -> Unit,
    private val onMdUpdate: (String) -> Unit,
    private val onStart: () -> Unit,
    private val onComplete: (String) -> Unit,
    private val onError: (Throwable?) -> Unit
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

    override fun onStart() {
        fullSourceAccumulator.setLength(0)
        scope.launch(Dispatchers.Main) { onStart() }
    }

    override fun onChunk(chunk: String) {
        fullSourceAccumulator.append(chunk)
        parser.onChunk(chunk)
    }

    override fun onComplete() {
        val fullSource = fullSourceAccumulator.toString()
        scope.launch(Dispatchers.Main) {
            // 4. 传给回调
            onComplete(fullSource)
        }
    }

    override fun onError(e: Throwable?) {
        scope.launch(Dispatchers.Main) { onError(e) }
    }
}

