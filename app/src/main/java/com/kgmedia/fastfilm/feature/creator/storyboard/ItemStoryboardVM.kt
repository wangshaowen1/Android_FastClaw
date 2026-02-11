package com.kgmedia.fastfilm.feature.creator.storyboard

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.widget.RecyclerEditText
import com.nhsd.basic.ext.showToast
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
    private val onItemChanged: () -> Unit,
    data: SimpleStoryboardInfo?
) : AppItemViewModel<SimpleStoryboardInfo>(data) {
    val no by lazy { ObservableInt() }
    val selected by lazy { ObservableBoolean(false) }

    /**
     * 当前应用的分镜描述
     */
    val genImagePrompt by lazy { ObservableField(data?.genImagePrompt) }

    /**
     * 编辑中的分镜描述
     */
    var editingGenImagePromptText: String? = data?.genImagePrompt
    val editingGenImagePrompt by lazy { ObservableBoolean(false) }
    val storyboardDescInputListener by lazy { ObservableField<RecyclerEditText.OnTextInputListener>() }
    private var loopGenStatusJob: Job? = null

    init {
        if (data?.imageGenerating == true) {
            loopGenStatus()
        }
    }

    override fun onCleared() {
        loopGenStatusJob?.cancel()
        super.onCleared()
    }

    fun clear() {
        loopGenStatusJob?.cancel()
    }

    fun editStoryboardDesc() {
        storyboardDescInputListener.set(object : RecyclerEditText.OnTextInputListener {
            override fun onTextInput(text: String) {
                editingGenImagePromptText = text
            }
        })
        editingGenImagePrompt.set(true)
        editingGenImagePromptText = genImagePrompt.get()
    }

    fun exitEditStoryboardDesc() {
        storyboardDescInputListener.set(null)
        editingGenImagePrompt.set(false)
        genImagePrompt.set(editingGenImagePromptText)
        saveInput()
    }

    private fun saveInput() {
        val genImagePrompt = this@ItemStoryboardVM.genImagePrompt.get()?.trim()
        if (genImagePrompt.isNullOrEmpty()) {
            return
        }
        launch {
            val body = SaveStoryboardBody(
                workId = workId,
                storyId = storyId,
                storyboardId = data.get()?.storyboardId,
                genImagePrompt = genImagePrompt
            )
            response(ApiFactory.apiService.workStoryboardSave(body)) {
                this@ItemStoryboardVM.data.set(data)
                onItemChanged()
            }
        }
    }

    fun generate() {
        if (data.get()?.imageGenerating == true) return
        val genImagePrompt = if (editingGenImagePrompt.get()) editingGenImagePromptText else this@ItemStoryboardVM.genImagePrompt.get()
        if (genImagePrompt.isNullOrEmpty()) {
            "请先填写分镜描述".showToast()
            return
        }
        val preStatus = data.get()?.genStatusImage
        data.get()?.genStatusImage = 1
        data.notifyChange()
        onItemChanged()
        launch {
            val body = GenerateStoryboardBody(
                storyboardId = data.get()?.storyboardId,
                workId = workId,
                storyId = storyId
            )
            response(
                bean = ApiFactory.apiService.workStoryboardSingleGenerate(body),
                result = {
                    val storyboard = this@ItemStoryboardVM.data.get() ?: SimpleStoryboardInfo()
                    storyboard.taskId = data?.taskId
                    storyboard.genStatusImage = data?.genStatus
                    this@ItemStoryboardVM.data.set(storyboard)
                    this@ItemStoryboardVM.data.notifyChange()
                    loopGenStatus()
                },
                error = {
                    this?.msg?.showToast()
                    data.get()?.genStatusImage = preStatus
                    onItemChanged()
                })
        }
    }

    private fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workStoryboardSingleStatus(TaskIdBody(taskId, 71)).data }
                data.get()?.let {
                    it.genStatusImage = taskStatus?.genStatus
                    it.imageUrl = taskStatus?.result
                }
                data.notifyChange()
            }
            if (data.get()?.imageGenerating == true) {
                delay(3000)
                loopGenStatus()
            } else {
                onItemChanged()
            }
        }
    }
}