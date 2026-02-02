package com.kgmedia.fastfilm.feature.creator.continuewriting

import android.graphics.Color
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.AIExpandBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkIdBody
import com.kgmedia.fastfilm.core.network.apis.response.LastStoryData
import com.kgmedia.fastfilm.core.network.apis.response.LastEpisodeData
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.PlainTextSseProcessor
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

class ContinueWritingViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "未命名标题",
        titleColor = R.color.textColorPrimary.getResColor(),
        background = Color.TRANSPARENT,
    )

    val workId by lazy { mBundle.getString(Constants.Extra.ID) }
    val workName by lazy { mBundle.getString(Constants.Extra.NAME) }
    val input by lazy { MutableLiveData(Global.continueWritingContent[workId]) }
    val info = MutableLiveData<LastStoryData>()
    val episodeInfo = MutableLiveData<LastEpisodeData>()
    val textLimitNum by lazy { Config.get().appConfig.shortDramas }
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(1000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val isVip = ObservableBoolean(true)
    val isSSEStreaming = ObservableBoolean(false)

    override fun onCreate() {
        super.onCreate()
        workName?.let {
            mToolbarVM.mTitle.set("续创：$it")
        }
        getDramaWorkInfo()
        getDramaInfo()
    }

    override fun onResume() {
        super.onResume()
        refreshConfig()
    }

    fun refreshConfig() {
//        isVip.set(User.get().isVip)
//        val maxLength = if (isVip.get()) textLimitNum?.shortDramasWriteStoryLimitNumForVip else textLimitNum?.shortDramasWriteStoryLimitNum
//        maxInputLength.set(maxLength ?: (if (isVip.get()) 5000 else 1000))
    }

    fun getDramaWorkInfo(function: (() -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.api.latestDramasWorkDetail(WorkIdBody(workId))) {
                info.value = data
                function?.invoke()
            }
        }
    }

    fun getDramaInfo(function: (() -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.api.dramaInfo(WorkIdBody(workId))) {
                episodeInfo.value = data
                mToolbarVM.mTitle.set(data?.workTitle)
            }
        }
    }

    private var sseHandler: SSEStreamHandler? = null

    fun sseGenerate() {
        promptGenerating.value = true to ("AI扩写中，请稍候...")
        val sseFlow = mRepo.api.sseContentAIExpand(AIExpandBody(info.value?.id, workId))
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
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    input.value = fullContent
                },
                onComplete = {
                    Logger.d("[SSE]", "Finished")
                    // 触发原本结束后的逻辑
                    isSSEStreaming.set(false)
                },
                onError = { e ->
                    Log.e("[SSE]", "Error: $e")
                    // 根据需要处理特定错误
                    e?.message?.showToast()
                    isSSEStreaming.set(false)
                })
        )
        // 3. 启动
        Logger.d("[SSE]", "Starting with content body = $input")
        sseHandler?.start(sseFlow)
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
    }
}