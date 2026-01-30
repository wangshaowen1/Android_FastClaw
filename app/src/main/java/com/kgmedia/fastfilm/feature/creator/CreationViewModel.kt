package com.kgmedia.fastfilm.feature.creator

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.apis.entity.InspirationInfo
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigData
import com.kgmedia.fastfilm.core.network.apis.request.CreationGeneratePreBody
import com.kgmedia.fastfilm.core.network.apis.request.SSEContentBody
import com.kgmedia.fastfilm.feature.creator.CreationFragment.Companion.KEY_MODEL
import com.kgmedia.fastfilm.feature.creator.CreationFragment.Companion.MODEL_INSPIRATION
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

class CreationViewModel : AppRepositoryViewModel<CreationRepository>() {
    val input by lazy { MutableLiveData<String?>() }
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(1000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val isVip = ObservableBoolean(User.get().isVip)
    val hint = ObservableField<String>()
    val creationModel by lazy {
        mBundle.getInt(KEY_MODEL)
    }

    val inspirationPrompts by lazy { MutableLiveData<List<InspirationInfo>>() }
    val videoSetConfig by lazy { MutableLiveData<VideoConfigData>() }

    fun inspirationModel(): Boolean {
        return creationModel == MODEL_INSPIRATION
    }

    override fun onCreate() {
        super.onCreate()
        input.value = if (inspirationModel()) Global.inspirationContent else Global.customizedContent
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

    private var sseHandler: SSEStreamHandler? = null

    fun sseGenerate(bizType: Int? = null, promptGeneratingContent: String?) {
        promptGenerating.value = true to (promptGeneratingContent ?: "")
        val sseFlow = mRepo.api.sseContent(SSEContentBody(input.value, bizType))
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            onStart = {
                promptGenerating.value = false to ""
                Logger.d("[SSE]", "Open")
                input.value = ""
            },
            onContentUpdate = { fullContent ->
                // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                input.value = fullContent
            },
            onComplete = {
                Logger.d("[SSE]", "Finished")
                // 触发原本结束后的逻辑
            },
            onError = { e ->
                Log.e("[SSE]", "Error: $e")
                // 根据需要处理特定错误
                e?.message?.showToast()
            }
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
        requestWithLoading({ mRepo.videoSetConfig() }, showLoading = showLoading) {
            videoSetConfig.value = data
            function?.invoke()
        }
    }

    fun randomInspirations(showLoading: Boolean = false) {
        requestWithLoading({ mRepo.getInspirationQuickList() }, showLoading = showLoading) {
            inspirationPrompts.value = data?.items
        }
    }

    fun nextStep(callback: (String?) -> Unit) {
        launchWithLoading(showLoading = true, {
            val promptResponse =
                mRepo.creationGeneratePre(
                    CreationGeneratePreBody(
                        if (inspirationModel()) CreationGeneratePreBody.GEN_WAY_INSPIRATION else CreationGeneratePreBody.GEN_WAY_CUSTOMIZED,
                        input.value,
                        null,
                        null,
                    )
                )
            if (promptResponse.isSuccess) {
                callback.invoke(promptResponse.data?.dramasId)
            } else {
                promptResponse.msg?.showToast()
            }
        }, onError = {
            parseMessage(it)?.showToast()
        })
    }
}