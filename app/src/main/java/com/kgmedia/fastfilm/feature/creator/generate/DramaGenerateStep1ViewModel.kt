package com.kgmedia.fastfilm.feature.creator.generate

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.utils.NotificationHelper
import com.kgmedia.fastfilm.core.ext.PollingState
import com.kgmedia.fastfilm.core.ext.launchPolling
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.ContinueTaskBody
import com.kgmedia.fastfilm.core.network.apis.request.PollSummaryData
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkIdBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import kotlinx.coroutines.Job

/**
 * 故事梗概生成
 */
class DramaGenerateStep1ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep1ViewModel"
        const val DISPLAY_FAIL = 1
        const val DISPLAY_CONTENT = 0
    }


    //小说文档扣费后生成的workId，用于轮询
    var workId: String? = null
    //任务Id
    var taskId: String? = null

    //业务类型
    var model: Int? = null

    // 作品名称
    var mWorkName = MutableLiveData<String>()

    val displayChild = ObservableInt(0)
    val isSummaryLoading = ObservableBoolean(true)
    val nextStepEnabled by lazy { ObservableBoolean(false) }

    /**
     * 故事梗概结果
     */
    var storySummaryResult = MutableLiveData<PollSummaryData?>()

    val isStoryDocumentsMode = ObservableBoolean(false)

    fun refreshDisplayContent() {
        val isDoc = model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS
        isStoryDocumentsMode.set(isDoc)
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        novelSummaryPollingJob?.cancel()
    }

    private var novelSummaryPollingJob: Job? = null

    /**
     * 轮询小说文档模式下的故事梗概
     * @param callback 是否是文档模式
     */
    fun pollSummary() {
        // 防抖与去重：如果已经在轮询中，直接 return
        if (novelSummaryPollingJob?.isActive == true) return
        isSummaryLoading.set(true)
        nextStepEnabled.set(false)
        displayChild.set(DISPLAY_CONTENT)
        storySummaryResult.value = null

        novelSummaryPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true, // 遇到断网等异常直接结束，不重试
            apiBlock = {
                if (isStoryDocumentsMode.get()) mRepo.api.pollNovelDocsSummary(WorkIdBody(workId)) else mRepo.api.pollSummaryResult(
                    TaskIdBody(
                        taskId
                    )
                )
            },
            checkState = { response ->
                val data = response?.data
                when {
                    data?.isSuccess() == true -> PollingState.SUCCESS
                    data?.isSensitive() == true -> PollingState.SUCCESS
                    data?.isFail() == true -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                response?.data?.result?.let {
                    nextStepEnabled.set(response.data?.isSensitive() == false)
                    mWorkName.value = it.workTitle ?: ""
                }
                storySummaryResult.value = response?.data
                isSummaryLoading.set(false)
            },
            onFail = { response, error ->
                displayChild.set(DISPLAY_FAIL)
                storySummaryResult.value = response?.data
                isSummaryLoading.set(false)
            },
            onPolling = { response ->
                storySummaryResult.value = response?.data
            }
        )
    }

    fun sendRecoveryNotification(content: String?) {
        val workName = mWorkName.value?.ifBlank { "未命名" } ?: "未命名"
        NotificationHelper.sendRecoveryNotification("《${workName}》${content}")
    }

    /**
     * 灵感故事梗概失败重新创建任务
     */
    fun retrySummary(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({ mRepo.api.retrySummaryTaskFromInspiration(ContinueTaskBody(workId, workType = 1)) }) {
            taskId = data?.taskId
            onSuccess?.invoke()
        }
    }

    fun reuploadStory(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({ mRepo.api.novelUploadDelete(WorkIdBody(workId)) }) {
            onSuccess?.invoke()
        }
    }

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
}