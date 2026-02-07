package com.kgmedia.fastfilm.feature.creator.assets

import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.SavePropBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ItemPropVM(
    workId: String?,
    storyId: String?,
    data: SimplePropInfo?
) : ItemAssetVM<SimplePropInfo>(workId, storyId, data) {

    override fun exitEditAssetDesc() {
        super.exitEditAssetDesc()
        saveInput()
    }

    override fun exitEditAssetName() {
        super.exitEditAssetName()
        saveInput()
    }

    private fun saveInput() {
        val propPrompt = assetDesc.get()?.trim()
        val propName = assetName.get()?.trim()
        if (propPrompt.isNullOrEmpty() && propName.isNullOrEmpty()) {
            return
        }
        launch {
            val body = SavePropBody(data.get()?.propId, data.get()?.appearanceId, workId, storyId, propName, propPrompt)
            response(ApiFactory.apiService.workAssetPropSave(body)) {
                this@ItemPropVM.data.set(data)
            }
        }
    }

    override val assetDescHint: String = "道具描述词"
    override val assetNameHint: String = "道具名"
    override val generateButtonText: String = "生成道具"

    override fun generate() {
        if (status.get() == 1) return
        val propPrompt = if (editingAssetDesc.get()) editingAssetDescText else assetDesc.get()
        val propName = if (editingAssetName.get()) editingAssetNameText else assetName.get()
        if (propPrompt.isNullOrEmpty()) {
            "请先填写道具描述".showToast()
            return
        }
        if (propName.isNullOrEmpty()) {
            "请先填写道具名称".showToast()
            return
        }
        status.set(1)
        launch {
            val body = GenerateAssetBody(
                assetId = data.get()?.propId,
                appearanceId = data.get()?.appearanceId,
                bizType = 51,
                prompt = propPrompt,
                referenceImageId = null,
                referenceImageInStatus = 0,
                referenceImageUrl = null,
                storyId = storyId,
                workId = workId,
            )
            response(
                bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                result = {
                    val prop = this@ItemPropVM.data.get() ?: SimplePropInfo()
                    prop.taskId = data?.taskId
                    prop.genStatus = data?.genStatus
                    this@ItemPropVM.data.set(prop)
                    loopGenStatus()
                },
                error = {
                    this?.msg?.showToast()
                    status.set(0)
                })
        }
    }

    override suspend fun deleteAsset(): ApiResponse<Any> {
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetPropDelete(IdsBody(null, data.get()?.appearanceId)) }
    }

    override fun loopGenStatus() {
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        launch {
            loop@ while (status.get() == 1) {
                runCatching {
                    val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, 51)).data }
                    data.get()?.let {
                        it.genStatus = taskStatus?.genStatus
                        it.imageUrl = taskStatus?.result
                        assetImage.set(taskStatus?.result)
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
