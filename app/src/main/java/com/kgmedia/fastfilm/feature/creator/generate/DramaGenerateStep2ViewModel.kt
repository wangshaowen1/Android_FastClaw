package com.kgmedia.fastfilm.feature.creator.generate

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.AIExpandBody
import com.kgmedia.fastfilm.core.network.apis.request.AISynopsisBody
import com.kgmedia.fastfilm.core.network.apis.request.CreateStoryWorkBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveEpisodeScriptBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.StorySseProcessor
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.utils.Logger

/**
 * 故事剧本生成
 */
class DramaGenerateStep2ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep2ViewModel"
        const val DISPLAY_LOADING = 0
        const val DISPLAY_FAIL = 1
        const val DISPLAY_CONTENT = 2
    }

    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(1000)
    }
    val input by lazy { MutableLiveData<String?>() }

    //业务类型
    var model: Int? = null
    var inspiration: String? = null

    //剧集集数code
    var episodeRangeCode: String? = null

    //自定义集数
    var customEpisodeCount: String? = null

    //视频比例code
    var videoRatio: String? = null

    //作品风格
    var videoStyleId: String? = null

    //作品ID
    var workId = MutableLiveData<String>()

    //剧集id
    var episodeId: String? = null
    //上一集的剧集id,只有续写的时候才有
    var previousStoryId: String? = null


    val nextStepEnabled by lazy { ObservableBoolean(false) }
    val sseOutputComplete by lazy { MutableLiveData(false) }
    val sseOutputFail by lazy { ObservableBoolean(false) }

    // 定义为成员变量
    private var sseHandler: SSEStreamHandler? = null

    //剧集剧本内容
    var originalJson: String? = null

    fun sseGenerate() {
        val sseFlow = if (model == DramaGenerateConstants.MODEL_STORY) {
            //第一集创作(小说)
            mRepo.api.sseScriptFromStory(
                AISynopsisBody(
                    inspiration,
                    episodeRangeCode,
                    customEpisodeCount
                )
            )
        } else {
            val body = AIExpandBody(
                workId = workId.value,
                storyId = if (model == DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION|| model == DramaGenerateConstants.MODEL_CONTINUE_STORY){
                    previousStoryId
                }else{
                    episodeId
                },
                bizType = model,
            )
            mRepo.api.sseScriptFromIdea(body)
        }
        promptGenerating.value = true to "内容生成中..."
        nextStepEnabled.set(false)
        sseOutputComplete.value = false
        sseOutputFail.set(false)
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = StorySseProcessor(
                scope = viewModelScope,
                onStart = {
                    promptGenerating.value = false to ""
                    input.value = ""
                    originalJson = ""
                    Logger.d("[SSE]", "Open")
                },
                onStoryUpdate = { fullContent ->
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    input.value = fullContent
                },
                onComplete = { originalContent ->
                    nextStepEnabled.set(true)
                    sseOutputComplete.value = true
                    originalJson = originalContent
                    Logger.d("[SSE]", "Finished")
                    // 触发原本结束后的逻辑
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    sseOutputComplete.value = true
                    sseOutputFail.set(true)
                    promptGenerating.value = false to ""
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
     * @param onSuccess 成功回调，返回作品ID
     */
    fun confirmGenerate(workTitle: String?, onSuccess: ((String?, String?) -> Unit)? = null) {
        launchWithLoading {
            if (model == DramaGenerateConstants.MODEL_STORY) {
                //第一集创作(小说)
                val body = CreateStoryWorkBody(
                    workTitle = workTitle,
                    workStyleCode = videoStyleId,
                    workRatio = videoRatio,
                    storyContentComplete = originalJson,
                    totalEpisodeCnt = customEpisodeCount,
                    totalEpisodeCntCode = episodeRangeCode
                )
                response(mRepo.api.saveStoryEpisodeScript(body)) {
                    onSuccess?.invoke(data?.workId, data?.storyId)
                }
            } else {
                val body = SaveEpisodeScriptBody(
                    workTitle = workTitle,
                    storyContentComplete = originalJson,
                    workId = workId.value,
                    storyId = episodeId,
                    workType = if (model == DramaGenerateConstants.MODEL_CONTINUE_STORY) 2 else 1
                )
                response(mRepo.api.saveEpisodeScript(body)) {
                    onSuccess?.invoke(data?.workId, data?.storyId)
                }
            }
        }
    }
}