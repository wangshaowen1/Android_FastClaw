package com.kgmedia.fastfilm.feature.creator.generate

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.PollingState
import com.kgmedia.fastfilm.core.ext.launchPolling
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.ContinueTaskBody
import com.kgmedia.fastfilm.core.network.apis.request.PollScriptBody
import com.kgmedia.fastfilm.core.network.apis.request.PollScriptData
import com.kgmedia.fastfilm.core.network.apis.request.SaveScriptBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.nhsd.basic.ext.getThemeColor
import kotlinx.coroutines.Job

/**
 * 故事剧本生成
 */
class DramaGenerateStep2ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep2ViewModel"
    }

    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(3500)
    }
    val input by lazy { MutableLiveData<String?>() }

    //业务类型
    var model: Int? = null

    //章节序号
    var chapterIndex: Int? = 1

    //作品ID
    var workId = MutableLiveData<String>()

    //剧集id
    var episodeId: String? = null

    //上一集的剧集id,只有续写的时候才有
    var previousStoryId: String? = null

    // 作品名称
    var mWorkName = MutableLiveData<String>()

    val nextStepEnabled by lazy { ObservableBoolean(false) }
    val outputComplete by lazy { MutableLiveData(false) }
    val outputFail by lazy { ObservableBoolean(false) }
    val isScriptLoading = ObservableBoolean(false)

    /**
     * 小说文档模式==》故事内容结果
     */
    var storyScriptResult = MutableLiveData<PollScriptData?>()

    //剧集剧本内容
    var originalJson: String? = null

    fun isNovelDocuments() =
        model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS || model == DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS

    fun createScriptTask(callBack: () -> Unit) {
        requestWithLoading({
            val body = ContinueTaskBody()
            when (model) {
                DramaGenerateConstants.MODEL_STORY_SINGLE -> mRepo.api.retryTaskFromSingleEpisode(body)
                DramaGenerateConstants.MODEL_STORY, DramaGenerateConstants.MODEL_CONTINUE_STORY -> mRepo.api.continueTaskFromNovel(body)
                DramaGenerateConstants.MODEL_INSPIRATION, DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION -> mRepo.api.createTaskFromInspiration(
                    body
                )

                else -> null
            }
        }) {
            callBack.invoke()
        }
    }

    private var scriptPollingJob: Job? = null

    /**
     * 轮询小说文档模式下的故事内容
     */
    fun pollNovelScript() {
        if (scriptPollingJob?.isActive == true) return

        resetPollingState()

        scriptPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true,
            apiBlock = {
                val body = PollScriptBody(workId.value, episodeId)
                if (isNovelDocuments()) {
                    mRepo.api.pollNovelScript(body)
                } else {
                    mRepo.api.pollScriptResult(body)
                }
            },
            checkState = { response ->
                when {
                    response?.data?.isSuccess() == true -> PollingState.SUCCESS
                    response?.data?.isFail() == true -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                handlePollingSuccess(response?.data)
            },
            onFail = { response, _ ->
                handlePollingFail(response?.data)
            }
        )
    }

    private fun resetPollingState() {
        isScriptLoading.set(true)
        nextStepEnabled.set(false)
        outputFail.set(false)
        outputComplete.value = false
        storyScriptResult.value = null
    }

    private fun handlePollingSuccess(data: PollScriptData?) {
        data?.storyContent?.let {
            nextStepEnabled.set(true)
            input.value = it
        }
        episodeId = data?.storyId
        chapterIndex = data?.sortNo
        storyScriptResult.value = data
        outputComplete.value = true
        isScriptLoading.set(false)
    }

    private fun handlePollingFail(data: PollScriptData?) {
        storyScriptResult.value = data
        isScriptLoading.set(false)
        outputFail.set(true)
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
        scriptPollingJob?.cancel()
    }

    /**
     * 确认生成
     * @param onSuccess 成功回调，返回作品ID
     */
    fun confirmGenerate(
        onSuccess: ((workId: String?, storyId: String?, taskId: String?) -> Unit)? = null
    ) {
        launchWithLoading {
            response(mRepo.api.saveScript(SaveScriptBody(workId.value, episodeId, input.value))) {
                generateAllAssetDescription(workId.value, episodeId, onSuccess)
            }
        }
    }

    private fun generateAllAssetDescription(
        workId: String?,
        storyId: String?,
        onSuccess: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?
    ) {
        launchWithLoading {
            response(mRepo.api.generateAllAssetDescription(WorkBody(workId, storyId))) {
                onSuccess?.invoke(workId, storyId, data?.taskId)
            }
        }
    }
}