package com.kgmedia.fastfilm.feature.creator.generate

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.AISynopsisBody
import com.kgmedia.fastfilm.core.network.apis.request.CreateWorkBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.EpisodeSseProcessor
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.utils.Logger

/**
 * 故事梗概生成
 */
class DramaGenerateStep1ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep1ViewModel"
        const val DISPLAY_FAIL = 1
        const val DISPLAY_CONTENT = 0
    }

    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()
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
    var mContent = ObservableField<String>()

    // 作品名称
    var mWorkName = MutableLiveData<String>()

    val displayChild = ObservableInt(DISPLAY_CONTENT)
    val nextStepEnabled by lazy { ObservableBoolean(false) }

    var onSSECompleted: (() -> Unit)? = null
    var originalJson: String? = null

    // 定义为成员变量
    private var sseHandler: SSEStreamHandler? = null

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
        displayChild.set(DISPLAY_CONTENT)
        promptGenerating.value = true to "故事生成中..."
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = EpisodeSseProcessor(
                scope = viewModelScope,
                onStart = {
                    originalJson = ""
                    mSSEContent.value = ""
                    mContent.set("")
                    Logger.d("[SSE]", "Open")
                },
                onTitleUpdate = { title ->
                    promptGenerating.value = false to ""
                    mWorkName.value = title
                },
                onMdUpdate = { fullContent ->
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    mSSEContent.value = fullContent
                },
                onComplete = { originalContent ->
                    Logger.d("[SSE]", "Finished")
                    Logger.d("[SSE]", "nextStepEnabled：${originalContent}")
                    if (originalContent.isNotBlank()) {
                        nextStepEnabled.set(true)
                    }
                    originalJson = originalContent
                    // 触发原本结束后的逻辑
                    onSSECompleted?.invoke()
                    promptGenerating.value = false to ""
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    displayChild.set(DISPLAY_FAIL)
                    onSSECompleted?.invoke()
                    promptGenerating.value = false to ""
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
                saveDesc(mdContent, data?.workId) {
                    onSuccess?.invoke(data?.workId, data?.storyId)
                }
            }
        }
    }

    fun saveDesc(mdContent: String?, workId: String?, onSuccess: (() -> Unit)? = null) {
        requestWithLoading({
            mRepo.api.updateWorkInfo(
                WorkInfoUpdateBody(
                    workId,
                    workDesc = parseSeasonOverview(mdContent ?: "")
                )
            )
        }) {
            onSuccess?.invoke()
        }
    }

    fun parseSeasonOverview(rawText: String): String {
        Logger.d("mdContent", rawText)
        val afterKeyword = rawText.substringAfter("整季梗概：", "")

        // 2. 找到第一个出现“第”字的地方（通常是 第1集 的开始）
        // 这样可以确保完全切断后续的集数内容
        val content = if (afterKeyword.contains("第")) {
            afterKeyword.substringBefore("第")
        } else {
            afterKeyword
        }

        // 3. 清理杂质：去除全角空格 (\u3000)、换行符、Markdown 符号
        return content
            .replace("\u3000", "") // 去除全角空格
            .replace(">", "")      // 去除可能存在的引用符
            .trim()                // 去除首尾不可见字符
    }
}