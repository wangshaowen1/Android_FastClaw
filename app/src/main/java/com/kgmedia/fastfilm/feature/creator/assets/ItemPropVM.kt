package com.kgmedia.fastfilm.feature.creator.assets

import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.SavePropBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast

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
                val prop = this@ItemPropVM.data.get() ?: SimplePropInfo()
                prop.appearanceId = data?.appearanceId
                prop.imageUrl = data?.imageUrl
                prop.propName = propName
                prop.propPrompt = propPrompt
                this@ItemPropVM.data.set(prop)
            }
        }
    }

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
            val body = GenerateAssetBody(51, propPrompt, data.get()?.appearanceId, null, null, null, workId, storyId)
            response(ApiFactory.apiService.workAssetSingleGenerate(body)) {
                val prop = this@ItemPropVM.data.get() ?: SimplePropInfo()
                prop.taskId = data?.taskId
                prop.genStatus = data?.genStatus
                this@ItemPropVM.data.set(prop)
            }
        }
    }

    override fun deleteAsset() {
        launch {
            response(ApiFactory.apiService.workAssetPropDelete(IdsBody(null, data.get()?.propId))) {
            }
        }
    }
}
