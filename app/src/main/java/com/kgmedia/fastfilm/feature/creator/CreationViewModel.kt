package com.kgmedia.fastfilm.feature.creator

import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.TextEncodingUtil
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.ext.showFirstRecharge
import com.kgmedia.fastfilm.core.ext.showRechargeDialog
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.InspirationInfo
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigData
import com.kgmedia.fastfilm.core.network.apis.request.CreateTaskBody
import com.kgmedia.fastfilm.core.network.apis.request.CreateTaskData
import com.kgmedia.fastfilm.core.network.apis.request.SSEContentBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.PlainTextSseProcessor
import com.kgmedia.fastfilm.feature.creator.CreationFragment.Companion.KEY_MODEL
import com.kgmedia.fastfilm.feature.creator.CreationFragment.Companion.MODEL_INSPIRATION
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.launch

class CreationViewModel : AppRepositoryViewModel<CreationRepository>() {
    val input by lazy { MutableLiveData<String?>() }
    var fileName: String? = null
    var fileUrl: String? = null
    var analysisResult: TextEncodingUtil.AnalysisResult? = null
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(1000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val hint = ObservableField<String>()
    val creationModel by lazy {
        mBundle.getInt(KEY_MODEL)
    }

    // 缓存当前批次尚未展示的数据池
    private val unshownInspirations = mutableListOf<InspirationInfo>()
    val inspirationPrompts by lazy { MutableLiveData<List<InspirationInfo>>() }
    val videoSetConfig by lazy { MutableLiveData<VideoConfigData>() }

    fun inspirationModel(): Boolean {
        return creationModel == MODEL_INSPIRATION
    }

    override fun onCreate() {
        super.onCreate()
        hint.set(if (inspirationModel()) "写下你的故事灵感，AI将帮你扩展成完整剧情" else "请粘贴一章完整的原创或已授权小说内容，用于改编创作")
        maxInputLength.set(if (inspirationModel()) 1000 else 2000)
        input.value =
            if (inspirationModel()) Global.inspirationContent else Global.customizedContent
        if (inspirationModel()) {
            randomInspirations()
        }
        videoSetConfig {

        }
    }

    @ColorInt
    fun getLimitColor(length: Int?): Int {
        val attr =
            if (length == maxInputLength.get()) R.attr.funcColorFailure else R.attr.textColor4
        return topActivity?.getThemeColor(attr) ?: Color.BLACK
    }

    private var sseHandler: SSEStreamHandler? = null

    fun isStreaming(): Boolean {
        return sseHandler?.isStreaming?.value ?: false
    }

    fun sseGenerate(bizType: Int? = null, promptGeneratingContent: String?) {
        promptGenerating.value = true to (promptGeneratingContent ?: "")
        val sseFlow =
            if (bizType == SSEContentBody.TYPE_RANDOM_INSPIRATION) mRepo.api.sseContentRandom(
                UserTokenBody()
            ) else mRepo.api.sseContentOptimize(
                SSEContentBody(input.value, bizType)
            )
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = PlainTextSseProcessor(
                scope = viewModelScope,
                onStart = {
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
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    // 根据需要处理特定错误
                    e?.message?.showToast()
                    promptGenerating.value = false to ""
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

    fun videoSetConfig(showLoading: Boolean = false, function: (() -> Unit)? = null) {
        if (videoSetConfig.value != null) {
            function?.invoke()
            return
        }
        requestWithLoading(
            apiBlock = { mRepo.videoSetConfig() },
            showLoading = showLoading
        ) {
            videoSetConfig.value = data
            function?.invoke()
        }
    }

    fun randomInspirations(showLoading: Boolean = false) {
        if (unshownInspirations.isNotEmpty()) {
            displayNextBatch()
            return
        }
        requestWithLoading(
            apiBlock = { mRepo.getInspirationQuickList() },
            showLoading = showLoading
        ) {
            // 判空并处理新数据
            data?.let { newData ->
                unshownInspirations.clear() // 保底清理
                // 将新拿到的一批数据打乱顺序后放入缓存池，这样后续按顺序取相当于随机抽取
                unshownInspirations.addAll(newData.shuffled())

                // 数据准备完毕，展示这一批的前5个
                displayNextBatch()
            }
        }
    }

    private fun displayNextBatch() {
        // 每次取最多 4 个。如果末尾剩余不足 4 个，就全部取出展示
        val countToTake = minOf(4, unshownInspirations.size)

        // 复制出要展示的数据列表
        val batchToShow = unshownInspirations.take(countToTake).toList()

        // 从缓存池中剔除已经取出的数据 (使用 subList.clear() 比 removeAll 更安全且性能更好)
        unshownInspirations.subList(0, countToTake).clear()

        // 更新 LiveData 通知 UI 渲染
        inspirationPrompts.value = batchToShow
    }

    fun createTask(modelType: Int, body: CreateTaskBody, callback: (CreateTaskData?) -> Unit) {
        requestWithLoading(
            apiBlock = {
                when (modelType) {
                    DramaGenerateConstants.MODEL_STORY_SINGLE -> {
                        mRepo.api.createTaskFromSingleEpisode(body)
                    }

                    DramaGenerateConstants.MODEL_INSPIRATION -> {
                        mRepo.api.createSummaryTaskFromInspiration(body)
                    }

                    DramaGenerateConstants.MODEL_STORY -> {
                        mRepo.api.createTaskFromNovel(body)
                    }

                    else -> null
                }
            },
            onSuccess = { callback.invoke(data) }
        )
    }

    fun confirmPowerConsume(body: UploadStoryBody, callback: (CreateTaskData?) -> Unit) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.novelUploadSummary(body)
            },
            onSuccess = { callback.invoke(data) },
            onError = {
                if (it is ApiException && it.errorCode == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                    viewModelScope.launch {
                        showRechargeDialog(true)
                    }
                } else {
                    it?.message?.showToast()
                }
            }
        )
    }

    fun extractStoryName(input: String?): String? {
        if (input.isNullOrBlank()) return null
        // 正则表达式解释：
        // 故事名称\s*[:：]\s* -> 匹配“故事名称”后跟可选空格，以及中英文冒号，再跟可选空格
        // 《(.*?)》            -> 匹配书名号，并捕获其中的内容（懒惰匹配）
        val regex = """故事名称\s*[:：]\s*《(.*?)》""".toRegex()

        val matchResult = regex.find(input)

        // 如果匹配成功，返回第一个捕获组的内容
        return matchResult?.groups?.get(1)?.value
    }
}