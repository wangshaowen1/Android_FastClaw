package com.kgmedia.fastfilm.feature.creator

import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.FileCountUtils
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.InspirationInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SensitiveWordCheckData
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigData
import com.kgmedia.fastfilm.core.network.apis.request.SSEContentBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.PlainTextSseProcessor
import com.kgmedia.fastfilm.feature.creator.CreationFragment.Companion.KEY_MODEL
import com.kgmedia.fastfilm.feature.creator.CreationFragment.Companion.MODEL_INSPIRATION
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

class CreationViewModel : AppRepositoryViewModel<CreationRepository>() {
    val input by lazy { MutableLiveData<String?>() }
    var fileName: String? = null
    var fileUrl: String? = null
    var analysisResult: FileCountUtils.AnalysisResult? = null
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(1000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val isVip = ObservableBoolean(true)
    val hint = ObservableField<String>()
    val creationModel by lazy {
        mBundle.getInt(KEY_MODEL)
    }

    val inspirationPrompts by lazy { MutableLiveData<List<InspirationInfo>>() }
    val videoSetConfig by lazy { MutableLiveData<VideoConfigData>() }

    //敏感词校验结果
    val sensitiveWordJudgmentResult by lazy { MutableLiveData<SensitiveWordCheckData?>() }

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

    override fun onResume() {
        super.onResume()
        refreshConfig()
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
                    parseMessage(e)?.showToast()
                    promptGenerating.value = false to ""
                })
        )
        // 3. 启动
        Logger.d("[SSE]", "Starting with content body = ${input.value}")
        sseHandler?.start(sseFlow)
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
    }

    fun refreshConfig() {
//        isVip.set(User.get().isVip)
//        val maxLength =
//            if (inspirationModel()) textLimitNum?.shortDramasInspirationWriteStoryLimitNum else (if (isVip.get()) textLimitNum?.shortDramasWriteStoryLimitNumForVip else textLimitNum?.shortDramasWriteStoryLimitNum)
//        maxInputLength.set(maxLength ?: (if (isVip.get()) 5000 else 1000))
//        hint.set(
//            if (inspirationModel()) textLimitNum?.shortDramasInspirationStoryRemindText
//                ?: getString(R.string.hint_inspiration_input) else textLimitNum?.shortDramasWriteStoryRemindText
//                ?: "请输入你想写的故事。例如：穿成恶毒女配，我决定躺平摆烂，每天在心里直播吐槽。没想到那个阴鸷狠戾的反派大佬，居然能听见我的心声。当我骂他“疯批”时，他却低声轻笑：“骂得好，再骂几句。”"
//        )
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
        requestWithLoading(
            apiBlock = { mRepo.getInspirationQuickList() },
            showLoading = showLoading
        ) {
            inspirationPrompts.value = data
        }
    }

    /**
     * 敏感词校验
     */
    fun sensitiveWordJudgment() {
        requestWithLoading(
            apiBlock = { mRepo.sensitiveWordCheck(input.value) },
            showLoading = true
        ) {
            sensitiveWordJudgmentResult.value = data
        }
    }

    fun confirmPowerConsume(callback: (UploadStoryData?) -> Unit) {
        requestWithLoading(
            apiBlock = { mRepo.api.novelUploadSummary(UploadStoryBody(analysisResult?.charCount, fileUrl)) }
        ) {
            callback.invoke(data)
        }
    }
}