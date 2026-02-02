package com.kgmedia.fastfilm.feature.creator.generate

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.AISynopsisBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.PlainTextSseProcessor
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.utils.Logger

/**
 * 故事梗概生成
 */
class DramaGenerateStep1ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep1ViewModel"
        const val DISPLAY_LOADING = 0
        const val DISPLAY_FAIL = 1
        const val DISPLAY_CONTENT = 2
    }

    var inspiration: String? = null
    var episodeRangeCode: String? = null
    var customEpisodeCount: String? = null
    var videoRatio: String? = null
    var videoStyleId: String? = null

    var mSSEContent = ObservableField<String>()
    var workId = MutableLiveData<String>()
    var mContent = ObservableField<String>()
    var mWorkName = MutableLiveData<String>()

    val displayChild = ObservableInt(DISPLAY_LOADING)
    val nextStepEnabled by lazy { ObservableBoolean(false) }

    // 定义为成员变量
    private var sseHandler: SSEStreamHandler? = null

    fun sseGenerate() {
        // 1. 准备请求 Flow
        val sseFlow = mRepo.api.sseContentSynopsis(AISynopsisBody(inspiration, episodeRangeCode, customEpisodeCount))

        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = PlainTextSseProcessor(
                scope = viewModelScope,
                onStart = {
                    displayChild.set(DISPLAY_CONTENT)
                    mSSEContent.set("")
                    mContent.set("")
                    Logger.d("[SSE]", "Open")
                },
                onUpdate = { fullContent ->
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    mSSEContent.set(fullContent)
                },
                onComplete = {
                    Logger.d("[SSE]", "Finished")
                    // 触发原本结束后的逻辑
                },
                onError = { e ->
                    Log.e("[SSE]", "Error: $e")
                    displayChild.set(DISPLAY_FAIL)
                    // 根据需要处理特定错误
                })
        )

        // 3. 启动
        Logger.d("[SSE]", "Starting with content body = $inspiration")
        sseHandler?.start(sseFlow)
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
    }

    /**
     * 确认生成
     * @param onSuccess 成功回调，返回剧本ID
     */
    fun confirmGenerate(onSuccess: ((String?) -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.shotDramaGenerate(workId.value)) {
                onSuccess?.invoke(data?.dramasId)
            }
        }
    }
}