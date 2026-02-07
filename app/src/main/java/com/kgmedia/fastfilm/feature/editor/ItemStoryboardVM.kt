package com.kgmedia.fastfilm.feature.editor

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast
import com.umeng.analytics.pro.da
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ItemStoryboardVM(
    /**
     * 作品 ID
     */
    val workId: String?,
    /**
     * 剧集 id
     */
    val storyId: String?,
    data: SimpleStoryboardInfo?
) : AppItemViewModel<SimpleStoryboardInfo>(data) {
    val selected by lazy { ObservableBoolean(false) }
    private var loopGenStatusJob: Job? = null

    init {
        if (data?.videoGenerating == true) {
            loopGenStatus()
        }
    }

    fun clear() {
        loopGenStatusJob?.cancel()
    }

    override fun onCleared() {
        loopGenStatusJob?.cancel()
        super.onCleared()
    }

    /**
     * [枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    fun genStatusBig(info: SimpleStoryboardInfo?) =
        when {
            info?.videoGenerating == true -> "视频生成中"
            info?.videoGenerateFailed == true -> "视频生成失败"
            else -> null
        }

    fun generate() {
        val storyboard = data.get() ?: return
        if (storyboard.videoGenerating) return
        val preStatus = data.get()?.genStatusVideo
        data.get()?.genStatusVideo = 1
        data.notifyChange()
        launch {
            val body = GenerateStoryboardBody(
                storyboardId = storyboard.storyboardId,
                workId = workId,
                storyId = storyId
            )
            response(
                bean = ApiFactory.apiService.workStoryboardVideoSingleGenerate(body),
                result = {
                    val storyboard = this@ItemStoryboardVM.data.get() ?: SimpleStoryboardInfo()
                    storyboard.taskId = data?.taskId
                    storyboard.genStatusVideo = data?.genStatus
                    this@ItemStoryboardVM.data.set(storyboard)
                    this@ItemStoryboardVM.data.notifyChange()
                    loopGenStatus()
                },
                error = {
                    this?.msg?.showToast()
                    data.get()?.genStatusVideo = preStatus
                })
        }
    }

    private fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        val storyboard = data.get()
        val taskId = storyboard?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workStoryboardSingleStatus(TaskIdBody(taskId, 72)).data }
                storyboard.genStatusVideo = taskStatus?.genStatus
                storyboard.videoUrl = taskStatus?.result
                data.notifyChange()
            }
            if (storyboard.videoGenerating) {
                delay(3000)
                loopGenStatus()
            }
        }
    }
}