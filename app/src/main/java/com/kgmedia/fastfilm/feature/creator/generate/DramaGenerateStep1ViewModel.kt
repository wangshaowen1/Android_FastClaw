package com.kgmedia.fastfilm.feature.creator.generate

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.utils.ProgressSimulator
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

    val promptGenerating = MutableLiveData<Triple<Boolean, String, String>>()
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

    private val progressHelper by lazy {
        ProgressSimulator(viewModelScope) { progress ->
            // 统一处理进度文案更新
            promptGenerating.value = Triple(true, "创建故事大纲 $progress%", "退出后AI会继续生成")
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
        displayChild.set(DISPLAY_CONTENT)
        promptGenerating.value = Triple(true, "创建故事大纲 0%", "退出后AI会继续生成")
        progressHelper.start(0, 5, 1000)
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
                    progressHelper.finish {
                        // 这里的代码会在 100% 显示 200ms 后执行
                        promptGenerating.value = Triple(false, "", "")
                    }
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
                    progressHelper.stop()
                    promptGenerating.value = Triple(false, "", "")
                    originalJson = originalContent
                    // 触发原本结束后的逻辑
                    onSSECompleted?.invoke()
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    displayChild.set(DISPLAY_FAIL)
                    onSSECompleted?.invoke()
                    progressHelper.stop()
                    promptGenerating.value = Triple(false, "", "")
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
                onSuccess?.invoke(data?.workId, data?.storyId)
//                saveDesc(mdContent, data?.workId) {
//                }
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