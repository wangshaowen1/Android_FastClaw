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
    private val onComplete: () -> Unit,
    private val onError: (Throwable?) -> Unit
) : SseChunkProcessor {

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
        scope.launch(Dispatchers.Main) { onStart() }
    }

    override fun onChunk(chunk: String) {
        parser.onChunk(chunk)
    }

    override fun onComplete() {
        scope.launch(Dispatchers.Main) { onComplete() }
    }

    override fun onError(e: Throwable?) {
        scope.launch(Dispatchers.Main) { onError(e) }
    }
}

