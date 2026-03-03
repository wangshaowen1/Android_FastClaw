package com.kgmedia.fastfilm.feature.creator.storyboard

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.sensitiveWordCheck
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
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
    val genImagePrompt by lazy { ObservableField<CharSequence>(data?.genImagePrompt) }

    /**
     * 编辑中的分镜描述
     */
    var editingGenImagePromptText: CharSequence? = data?.genImagePrompt
        private set
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
    }

    fun clickGenerate() {
        if (data.get()?.imageGenerating == true) return
        val prompt = if (editingGenImagePrompt.get()) editingGenImagePromptText else genImagePrompt.get()
        if (prompt.isNullOrEmpty()) {
            "请先填写分镜描述".showToast()
            return
        }
        generate(prompt.toString())
    }

    fun generate(prompt: String) {
        if (data.get()?.imageGenerating == true) return
        val preStatus = data.get()?.genStatusImage
        data.get()?.genStatusImage = 1
        data.notifyChange()
        onItemChanged()
        launch {
            var storyboard = data.get()
            if (prompt != storyboard?.genImagePrompt) {
                sensitiveWordCheck(prompt).let { (isPass, highlightText) ->
                    if (!isPass) {
                        highlightText?.let { genImagePrompt.set(it) }
                        data.get()?.genStatusImage = preStatus
                        data.notifyChange()
                        return@launch
                    }
                }
                val response = withContext(Dispatchers.IO) {
                    val body = SaveStoryboardBody(
                        workId = workId,
                        storyId = storyId,
                        storyboardId = storyboard?.storyboardId,
                        genImagePrompt = prompt
                    )
                    ApiFactory.apiService.workStoryboardSave(body)
                }
                if (response.isSuccess && response.data != null) {
                    storyboard = response.data
                    data.set(response.data)
                } else {
                    response.msg?.showToast()
                    data.get()?.genStatusImage = preStatus
                    data.notifyChange()
                    onItemChanged()
                    return@launch
                }
            }
            val body = GenerateStoryboardBody(
                workId = workId,
                storyId = storyId,
                storyboardId = storyboard?.storyboardId,
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
                    if (this?.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                        PowerRechargeDialog.show(true)
                    } else {
                        this?.msg?.showToast()
                    }
                    data.get()?.genStatusImage = preStatus
                    data.notifyChange()
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