package com.kgmedia.fastfilm.feature.creator.continuewriting

import android.graphics.Color
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SaveSequelNovelStoryInfo
import com.kgmedia.fastfilm.core.network.apis.request.AIExpandBody
import com.kgmedia.fastfilm.core.network.apis.request.LatestEpisodeBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveSequelNovelStoryBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.PlainTextSseProcessor
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

class ContinueWritingViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "未命名标题",
        background = Color.TRANSPARENT,
    )

    val workId by lazy { mBundle.getString(Constants.Extra.ID) }
    val workName by lazy { mBundle.getString(Constants.Extra.NAME) }
    val input by lazy { MutableLiveData(Global.continueWritingContent[workId]) }
    val episodeInfo = MutableLiveData<EpisodeInfo>()

    val textLimitNum by lazy { Config.get().appConfig.shortDramas }
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(1000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val isVip = ObservableBoolean(true)
    val isSSEStreaming = ObservableBoolean(false)

    //标题
    val combinationTitle = ObservableField("")

    private var sseHandler: SSEStreamHandler? = null


    override fun onCreate() {
        super.onCreate()
        workName?.let {
            mToolbarVM.mTitle.set("续创：$it")
        }
        getEpisodeInfo()
    }

    /**
     *获取剧集最新一集剧本详情
     */
    fun getEpisodeInfo(function: (() -> Unit)? = null) {
        launchWithLoading {
            response(
                mRepo.api.latestEpisode(
                    LatestEpisodeBody(
                        workType = LatestEpisodeBody.WORK_TYPE_NOVEL,
                        workId = workId
                    )
                )
            ) {
                episodeInfo.value = data
                gradeAssembly()
                function?.invoke()
            }
        }
    }

    fun sseGenerate() {
        promptGenerating.value = true to ("AI扩写中...")
        val sseFlow = mRepo.api.sseContentAIExpand(
            AIExpandBody(
                storyId = episodeInfo.value?.storyId,
                workId = workId,
                bizType = DramaGenerateConstants.MODEL_CONTINUE_STORY_EXPAND
            )
        )
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = PlainTextSseProcessor(
                scope = viewModelScope,
                onStart = {
                    isSSEStreaming.set(true)
                    promptGenerating.value = false to ""
                    Logger.d("[SSE]", "Open")
                    input.value = ""
                },
                onUpdate = { fullContent ->
                    Logger.d("[SSE]", "Update: $fullContent")
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    input.value = fullContent
                },
                onComplete = {
                    Logger.d("[SSE]", "Finished")
                    // 触发原本结束后的逻辑
                    isSSEStreaming.set(false)
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    // 根据需要处理特定错误
                    promptGenerating.value = false to ""
                    e?.message?.showToast()
                    isSSEStreaming.set(false)
                })
        )
        // 3. 启动
        Logger.d("[SSE]", "Starting with content body = $input")
        sseHandler?.start(sseFlow)
    }


    private fun gradeAssembly() {
        val episode = episodeInfo.value
        if (episode == null) {
            combinationTitle.set("")
            return
        }
        combinationTitle.set(buildString {
            append("第")
            append(episode.sortNo)
            append("集")
            if (!episode.storyTitle.isNullOrBlank()) {
                append(" | ")
                append(episode.storyTitle)
            }
        })
    }


    /**
     *小说-保存续创剧集（只有小说有）
     */
    fun saveSequelNovelStory(function: ((SaveSequelNovelStoryInfo) -> Unit)? = null) {
        launchWithLoading {
            response(
                mRepo.api.saveSequelNovelStory(
                    SaveSequelNovelStoryBody(
                        storySummary = input.value,
                        workId = workId,
                        storyId = episodeInfo.value?.storyId
                    )
                )
            ) {
                function?.invoke(data)
            }
        }
    }


    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
    }
}