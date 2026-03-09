package com.kgmedia.fastfilm.feature.creator.generate

import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.AutoProgressSimulator
import com.kgmedia.fastfilm.common.utils.NotificationHelper
import com.kgmedia.fastfilm.core.ext.PollingState
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchPolling
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.network.apis.request.AIExpandBody
import com.kgmedia.fastfilm.core.network.apis.request.AISynopsisBody
import com.kgmedia.fastfilm.core.network.apis.request.ContentBody
import com.kgmedia.fastfilm.core.network.apis.request.CreateStoryWorkBody
import com.kgmedia.fastfilm.core.network.apis.request.NovelScriptGenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.PollNovelScriptData
import com.kgmedia.fastfilm.core.network.apis.request.SaveEpisodeScriptBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.StorySseProcessor
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Job

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

    val promptGenerating = MutableLiveData<Triple<Boolean, String, String>>()
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(2000)
    }
    val input by lazy { MutableLiveData<String?>() }

    //业务类型
    var model: Int? = null

    //灵感内容
    var inspiration: String? = null

    //剧集集数code
    var episodeRangeCode: String? = null

    //自定义集数
    var customEpisodeCount: String? = null

    //视频比例code
    var videoRatio: String? = null

    //作品风格
    var videoStyleId: String? = null

    //章节序号
    var chapterIndex: Int? = null

    //作品ID
    var workId = MutableLiveData<String>()

    //剧集id
    var episodeId: String? = null

    //上一集的剧集id,只有续写的时候才有
    var previousStoryId: String? = null

    // 作品名称
    var mWorkName = MutableLiveData<String>()

    val nextStepEnabled by lazy { ObservableBoolean(false) }
    val sseOutputComplete by lazy { MutableLiveData(false) }
    val sseOutputFail by lazy { ObservableBoolean(false) }
    val isScriptLoading = ObservableBoolean(true)

    /**
     * 小说文档模式==》故事内容结果
     */
    var storyScriptResult = MutableLiveData<PollNovelScriptData?>()

    // 定义为成员变量
    private var sseHandler: SSEStreamHandler? = null

    //剧集剧本内容
    var originalJson: String? = null

    private val progressHelper by lazy {
        AutoProgressSimulator(viewModelScope) { show, title, progress ->
            // 统一处理进度文案更新
            promptGenerating.value = Triple(show, "$title $progress%", "退出后AI会继续生成")
        }
    }

    fun isNovelDocuments() =
        model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS || model == DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS

    fun isStreaming() = sseHandler?.isStreaming?.value == true

    fun sseGenerate() {
        val sseFlow =
            if (model == DramaGenerateConstants.MODEL_STORY || model == DramaGenerateConstants.MODEL_CONTINUE_STORY) {
                //第一集创作(小说)
                val body = AISynopsisBody(
                    workInspiration = inspiration,
                    totalEpisodeCntCode = AISynopsisBody.CUSTOM_EXACT,
                    totalEpisodeCnt = "1"
                )
                Logger.d("[SSE]", "MODEL_STORY Starting with content body = $body")
                mRepo.api.sseScriptFromStory(body)
            } else if (model == DramaGenerateConstants.MODEL_STORY_SINGLE) {
                Logger.d("[SSE]", "MODEL_STORY_SINGLE Starting with content body = $inspiration")
                mRepo.api.singleWorkGenerate(ContentBody(inspiration))
            } else {
                val body = AIExpandBody(
                    workId = workId.value,
                    storyId = if (model == DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION || model == DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS) {
                        previousStoryId
                    } else {
                        episodeId
                    },
                )
                Logger.d("[SSE]", "MODEL_CONTINUE_INSPIRATION Starting with content body = $body")
                mRepo.api.sseScriptFromIdea(body)
            }
        progressHelper.start(
            "生成故事剧本",
            step = 2,
            intervalMs = 100L
        )
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
                    input.value = ""
                    originalJson = ""
                    Logger.d("[SSE]", "Open")
                },
                onTitleBack = { title ->
                    progressHelper.finish()
                    mWorkName.value = parseCleanTitle(title)
                },
                onStoryUpdate = { fullContent ->
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    input.value = fullContent
                    progressHelper.stop()
                },
                onComplete = { originalContent ->
                    nextStepEnabled.set(true)
                    sseOutputComplete.value = true
                    originalJson = originalContent
                    progressHelper.stop()
                    Logger.d("[SSE]", "Finished originalContent：${originalContent}")
                    // 触发原本结束后的逻辑
                    NotificationHelper.sendRecoveryNotification("《${mWorkName.value?.ifEmpty { "未命名" }}》第${chapterIndex}集故事剧本已完成")
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    sseOutputComplete.value = true
                    sseOutputFail.set(true)
                    progressHelper.stop()
                    parseMessage(e)?.showToast()
                    val workName = mWorkName.value?.ifBlank { "未命名" } ?: "未命名"
                    NotificationHelper.sendRecoveryNotification("《${workName}》第${chapterIndex}集故事剧本生成失败")
                    // 根据需要处理特定错误
                })
        )

        // 3. 启动
        Logger.d("[SSE]", "启动 灵感内容 = $inspiration")
        sseHandler?.start(sseFlow)
    }

    private var novelScriptPollingJob: Job? = null

    /**
     * 轮询小说文档模式下的故事内容
     */
    fun pollNovelScript() {
        // 防抖与去重：如果已经在轮询中，直接 return
        if (novelScriptPollingJob?.isActive == true) return
        isScriptLoading.set(true)
        nextStepEnabled.set(false)
        sseOutputFail.set(false)
        sseOutputComplete.value = false
        storyScriptResult.value = null

        novelScriptPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true, // 遇到断网等异常直接结束，不重试
            apiBlock = { mRepo.api.pollNovelScript(NovelScriptGenerateBody(workId.value, chapterIndex ?: 1)) },
            checkState = { response ->
                val data = response?.data
                when {
                    data?.isSuccess() == true -> PollingState.SUCCESS
                    data?.isFail() == true -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                response?.data?.storyContent?.let {
                    nextStepEnabled.set(true)
                    input.value = it
                }
                sseOutputComplete.value = true
                storyScriptResult.value = response?.data
                isScriptLoading.set(false)
            },
            onFail = { response, error ->
                storyScriptResult.value = response?.data
                isScriptLoading.set(false)
                sseOutputFail.set(true)
            }
        )
    }


    fun parseCleanTitle(rawTitle: String): String {
        // 正则解释：
        // ^      : 匹配字符串开头
        // 第     : 匹配“第”字
        // \d+    : 匹配一个或多个数字
        // 集     : 匹配“集”字
        // [：:\s]* : 匹配后面可能跟着的 中文冒号、英文冒号 或 空格 (0次或多次)
        val regex = Regex("^第\\d+集[：:\\s]*")

        return rawTitle.replace(regex, "")
    }

    @ColorInt
    fun getLimitColor(length: Int?): Int {
        val attr = if (length == maxInputLength.get()) R.attr.funcColorFailure else R.attr.textColor4
        return topActivity?.getThemeColor(attr) ?: Color.BLACK
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
        novelScriptPollingJob?.cancel()
    }

    /**
     * 确认生成
     * @param onSuccess 成功回调，返回作品ID
     */
    fun confirmGenerate(
        workTitle: String?,
        onSuccess: ((workId: String?, storyId: String?, taskId: String?) -> Unit)? = null
    ) {
        setLoading(true)
        launch(block = {
            if (model == DramaGenerateConstants.MODEL_STORY || model == DramaGenerateConstants.MODEL_STORY_SINGLE) {
                //第一集创作(小说)
                val body = CreateStoryWorkBody(
                    workTitle = workTitle,
                    workStyleCode = videoStyleId,
                    workRatio = videoRatio,
                    storyContentComplete = originalJson,
                    totalEpisodeCnt = customEpisodeCount,
                    totalEpisodeCntCode = episodeRangeCode
                )
                response(
                    bean = if (model == DramaGenerateConstants.MODEL_STORY) mRepo.api.saveStoryEpisodeScript(body) else mRepo.api.saveSingleEpisodeScript(
                        body
                    ),
                    result = {
                        generateAllAssetDescription(
                            data?.workId,
                            data?.storyId,
                            onSuccess
                        )
                    },
                    error = {
                        setLoading(false)
                        this?.msg?.showToast()
                    })
            } else {
                val body = SaveEpisodeScriptBody(
                    workTitle = workTitle,
                    storyContentComplete = originalJson,
                    workId = workId.value,
                    storyId = episodeId,
                    workType = if (model == DramaGenerateConstants.MODEL_CONTINUE_STORY) 2 else 1
                )
                response(
                    bean = mRepo.api.saveEpisodeScript(body),
                    result = {
                        generateAllAssetDescription(
                            data?.workId,
                            data?.storyId,
                            onSuccess
                        )
                    },
                    error = {
                        setLoading(false)
                        this?.msg?.showToast()
                    })
            }
        }, onError = {
            setLoading(false)
        })
    }

    private fun generateAllAssetDescription(
        workId: String?,
        storyId: String?,
        onSuccess: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?
    ) {
        launch {
            response(
                bean = mRepo.api.generateAllAssetDescription(WorkBody(workId, storyId)),
                result = {
                    setLoading(false)
                    onSuccess?.invoke(workId, storyId, data?.taskId)
                },
                error = {
                    setLoading(false)
                    this?.msg?.showToast()
                })
        }
    }
}