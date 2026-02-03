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
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.widget.RecyclerEditText
import com.nhsd.basic.ext.showToast

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
        val storyboardPrompt = if (editingStoryboardDesc.get()) editingStoryboardDescText else storyboardDesc.get()
        if (storyboardPrompt.isNullOrEmpty()) {
            "请先填写分镜描述".showToast()
            return
        }
        status.set(1)
        launch {
            val body = GenerateStoryboardBody(data.get()?.storyboardId, storyboardPrompt, null, null, workId, storyId)
            response(ApiFactory.apiService.workStoryboardSingleGenerate(body)) {
                val role = this@ItemStoryboardVM.data.get() ?: SimpleStoryboardInfo()
                role.taskId = data?.taskId
                role.stepStatus = data?.genStatus
                this@ItemStoryboardVM.data.set(role)
            }
        }
    }

    fun deleteAsset() {
        launch {
            response(ApiFactory.apiService.workStoryboardDelete(IdsBody(null, data.get()?.storyboardId))) {
            }
        }
    }
}