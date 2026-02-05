package com.kgmedia.fastfilm.feature.creator.storyboard

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.widget.RecyclerEditText
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
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

    /**
     * -1=新创建 0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成
     */
    val status by lazy { ObservableInt(data?.stepStatus ?: -1) }

    /**
     * 当前应用的分镜描述
     */
    val storyboardDesc by lazy { ObservableField(data?.storyBoardDesc) }

    /**
     * 编辑中的分镜描述
     */
    var editingStoryboardDescText: String? = data?.storyBoardDesc
    val editingStoryboardDesc by lazy { ObservableBoolean(false) }
    val storyboardDescInputListener by lazy { ObservableField<RecyclerEditText.OnTextInputListener>() }

    init {
        if (status.get() == 1) {
            loopGenStatus()
        }
    }

    /**
     * [枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    fun genStatusBig(status: Int?) =
        when (status) {
            0, 1, 2 -> "视频生成中"
            3 -> "视频生成失败"
            else -> null
        }

    fun editAssetDesc() {
        storyboardDescInputListener.set(object : RecyclerEditText.OnTextInputListener {
            override fun onTextInput(text: String) {
                editingStoryboardDescText = text
            }
        })
        editingStoryboardDesc.set(true)
        editingStoryboardDescText = storyboardDesc.get()
    }

    fun exitEditAssetDesc() {
        storyboardDescInputListener.set(null)
        editingStoryboardDesc.set(false)
        storyboardDesc.set(editingStoryboardDescText)
        saveInput()
    }

    private fun saveInput() {
        val storyboardPrompt = storyboardDesc.get()?.trim()
        if (storyboardPrompt.isNullOrEmpty()) {
            return
        }
        launch {
            val body = SaveStoryboardBody(data.get()?.storyboardId, storyboardPrompt, null, null, null, null, null, workId, storyId)
            response(ApiFactory.apiService.workStoryboardSave(body)) {
                this@ItemStoryboardVM.data.set(data)
            }
        }
    }

    fun generate() {
        if (status.get() == 1) return
        val storyBoardDesc = if (editingStoryboardDesc.get()) editingStoryboardDescText else storyboardDesc.get()
        if (storyBoardDesc.isNullOrEmpty()) {
            "请先填写分镜描述".showToast()
            return
        }
        status.set(1)
        launch {
            val body = GenerateStoryboardBody(
                storyboardId = data.get()?.storyboardId,
                storyBoardDesc = storyBoardDesc,
                genImagePrompt = data.get()?.genImagePrompt,
                genVideoPrompt = null,
                workId = workId,
                storyId = storyId
            )
            response(ApiFactory.apiService.workStoryboardSingleGenerate(body)) {
                val storyboard = this@ItemStoryboardVM.data.get() ?: SimpleStoryboardInfo()
                storyboard.taskId = data?.taskId
                storyboard.stepStatus = data?.genStatus
                data?.genStatus?.let { status.set(it) }
                this@ItemStoryboardVM.data.set(storyboard)
                loopGenStatus()
            }
        }
    }

    fun deleteAsset() {
        launch {
            response(ApiFactory.apiService.workStoryboardDelete(IdsBody(null, data.get()?.storyboardId))) {
            }
        }
    }

    private fun loopGenStatus() {
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        launch {
            loop@ while (status.get() == 1) {
                runCatching {
                    val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, 71)).data }
                    data.get()?.let {
                        it.stepStatus = taskStatus?.genStatus
                        it.imageUrl = taskStatus?.result
                    }
                    data.notifyChange()
                    taskStatus?.genStatus?.let { status.set(it) }
                }
                if (status.get() != 1) {
                    break@loop
                }
                delay(3000)
            }
        }
    }
}