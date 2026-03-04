package com.kgmedia.fastfilm.feature.creator.generate

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.utils.AutoProgressSimulator
import com.kgmedia.fastfilm.common.utils.NotificationHelper
import com.kgmedia.fastfilm.core.ext.PollingState
import com.kgmedia.fastfilm.core.ext.launchPolling
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.AISynopsisBody
import com.kgmedia.fastfilm.core.network.apis.request.CreateWorkBody
import com.kgmedia.fastfilm.core.network.apis.request.RollNovelSummaryResult
import com.kgmedia.fastfilm.core.network.apis.request.WorkIdBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.EpisodeSseProcessor
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Job

/**
 * 故事梗概生成
 */
class DramaGenerateStep1ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep1ViewModel"
    }

    var displayFail = 1
    var displayContent = 0

    val promptGenerating = MutableLiveData<Triple<Boolean, String, String>>()

    //小说文档扣费后生成的workId，用于轮询
    var workId: String? = null

    //业务类型
    var model: Int? = null
    var inspiration: String? = null
    var episodeRangeCode: String? = null

    //自定义集数
    var customEpisodeCount: String? = null

    //视频比例
    var videoRatio: String? = null

    //视频风格样式id
    var videoStyleId: String? = null

    //作品内容
    var mSSEContent = MutableLiveData<String>()

    // 作品名称
    var mWorkName = MutableLiveData<String>()

    val displayChild = ObservableInt(0)
    val nextStepEnabled by lazy { ObservableBoolean(false) }

    var onSSECompleted: (() -> Unit)? = null
    var originalJson: String? = null

    /**
     * 小说文档模式==》故事梗概结果
     */
    var storySummaryResult = MutableLiveData<RollNovelSummaryResult>()

    // 定义为成员变量
    private var sseHandler: SSEStreamHandler? = null

    fun isStoryDocumentsMode() = model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS

    override fun onCreate() {
        super.onCreate()
        displayContent = if(isStoryDocumentsMode()) 1 else 0
        displayFail = if(isStoryDocumentsMode()) 2 else 1
    }

    private val progressHelper by lazy {
        AutoProgressSimulator(viewModelScope) { show, title, progress ->
            // 统一处理进度文案更新
            promptGenerating.value = Triple(show, "$title $progress%", "退出后AI会继续生成")
        }
    }

    fun sseGenerate() {
        nextStepEnabled.set(false)
        // 1. 准备请求 Flow
        val sseFlow = mRepo.api.sseContentSynopsis(
            AISynopsisBody(
                inspiration,
                episodeRangeCode,
                customEpisodeCount
            )
        )
        displayChild.set(displayContent)
        progressHelper.start(
            "创建故事大纲", "生成故事梗概",
            step = 1,
            intervalMs = 100L
        )
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = EpisodeSseProcessor(
                scope = viewModelScope,
                onStart = {
                    originalJson = ""
                    mSSEContent.value = ""
                    Logger.d("[SSE]", "Open")
                },
                onTitleUpdate = { title ->
                    progressHelper.finish()
                    mWorkName.value = title
                },
                onMdUpdate = { delta ->
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    mSSEContent.value = delta
                    Logger.d("[SSE]", "onMdUpdate：$delta")
                },
                onComplete = { originalContent ->
                    Logger.d("[SSE]", "Finished")
                    Logger.d("[SSE]", "nextStepEnabled：${originalContent}")
                    if (originalContent.isNotBlank()) {
                        nextStepEnabled.set(true)
                    }
                    progressHelper.stop()
                    originalJson = originalContent
                    // 触发原本结束后的逻辑
                    onSSECompleted?.invoke()
                    NotificationHelper.sendRecoveryNotification("故事梗概已完成")
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    displayChild.set(displayFail)
                    onSSECompleted?.invoke()
                    parseMessage(e)?.showToast()
                    progressHelper.stop()
                    NotificationHelper.sendRecoveryNotification("故事梗概生成失败")
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
        progressHelper.stop()
        novelSummaryPollingJob?.cancel()
        sseHandler?.cancel()
    }

    private var novelSummaryPollingJob: Job? = null

    /**
     * 轮询小说文档模式下的故事梗概
     */
    fun rollNovelSummary() {
        // 防抖与去重：如果已经在轮询中，直接 return
        if (novelSummaryPollingJob?.isActive == true) return

        nextStepEnabled.set(false)
        displayChild.set(displayContent)
        progressHelper.start(
            "创建故事大纲", "生成故事梗概",
            step = 1,
            intervalMs = 6000L
        )

        novelSummaryPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true, // 遇到断网等异常直接结束，不重试
            apiBlock = { mRepo.api.rollNovelSummary(WorkIdBody(workId)) },
            checkState = { response ->
                val data = response?.data
                when {
                    data?.isSuccess() == true -> PollingState.SUCCESS
                    data?.isFail() == true -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                response?.data?.result?.let {
                    nextStepEnabled.set(true)
                    mWorkName.value = it.workTitle ?: ""
                    storySummaryResult.value = it
                }
                progressHelper.finish()
                NotificationHelper.sendRecoveryNotification("故事梗概已完成")
            },
            onFail = { _, error ->
                displayChild.set(displayFail)
                progressHelper.stop()
                NotificationHelper.sendRecoveryNotification("故事梗概生成失败")
            }
        )
    }

    /**
     * 确认生成
     * @param onSuccess 成功回调，返回作品ID
     */
    fun confirmGenerate(mdContent: String?, onSuccess: ((String?, String?) -> Unit)? = null) {
        launchWithLoading {
            val body = CreateWorkBody(
                workTitle = mWorkName.value,
                storyStructureStr = originalJson,
                workStyleCode = videoStyleId,
                workRatio = videoRatio,
                totalEpisodeCnt = customEpisodeCount,
                totalEpisodeCntCode = episodeRangeCode
            )
            response(mRepo.api.saveWorkSummary(body)) {
                onSuccess?.invoke(data?.workId, data?.storyId)
            }
        }
    }
//
//
//    /**
//     * 敏感词校验
//     */
//    fun sensitiveWordJudgment(callback: () -> Unit) {
//        requestWithLoading(
//            apiBlock = { mRepo.sensitiveWordCheck(mSSEContent.value) },
//            showLoading = true
//        ) {
//            if (data?.isSensitive() == true) {
//                data?.content?.showToast()
//            } else {
//                callback.invoke()
//            }
//        }
//    }

    fun saveTitle(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({
            mRepo.api.updateWorkInfo(
                WorkInfoUpdateBody(
                    workId,
                    workTitle = mWorkName.value
                )
            )
        }) {
            onSuccess?.invoke()
        }
    }

    fun parseSeasonOverview(rawText: String): String {
        // 1. 预处理：标准化空白字符
        // 把所有的 全角空格(\u3000)、制表符(\t)、换行(\n) 都变成单个普通空格
        // 这样 "整季梗概：\n   \u3000  内容" 就会变成 "整季梗概： 内容"
        val normalizedText = rawText.replace(Regex("[\\s\\u3000]+"), " ")

        // 2. 定位起点
        val startKeyword = "整季梗概："
        val startIndex = normalizedText.indexOf(startKeyword)
        if (startIndex == -1) return "" // 没找到直接返回空

        // 截取起点之后的所有内容（此时内容已经是单行，没有乱七八糟的符号了）
        val contentAfterStart = normalizedText.substring(startIndex + startKeyword.length)

        // 3. 智能定位终点（核心优化）
        // 匹配模式： "第" + (空格可选) + (阿拉伯数字 或 中文数字) + (空格可选) + "集"
        // 例子能匹配： "第1集", "第一集", "第 1 集", "第 十二 集"
        val nextEpisodeRegex = Regex("第\\s*[0-9一二三四五六七八九十]+\\s*集")

        val matchResult = nextEpisodeRegex.find(contentAfterStart)

        val finalContent = if (matchResult != null) {
            // 找到了下一集的开头，截取到这里为止
            contentAfterStart.take(matchResult.range.first)
        } else {
            // 没找到下一集（可能只有梗概），直接取全部
            contentAfterStart
        }

        // 4. 最终净化
        return finalContent
            .replace(">", "")      // 去除 Markdown 引用符
            .replace("*", "")      // 去除 Markdown 加粗符
            .trim()                // 去除首尾空格
    }
}