package com.kgmedia.fastfilm.feature.creator.assets

import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.request.SaveRoleBody
import com.kgmedia.fastfilm.core.response

class ItemRoleVM(data: SimpleRoleInfo?, create: Boolean = false) : ItemAssetVM<SimpleRoleInfo>(data, create) {
    override fun generate() {
        if (status.get() == 1) return
        val assetDesc = if (editingAssetDesc.get()) editingAssetDescText else assetDesc.get()
        val assetName = if (editingAssetName.get()) editingAssetNameText else assetName.get()
        status.set(1)
        launch {
            val body = SaveRoleBody(data.get()?.roleId, data.get()?.appearanceId, null, null, assetName, assetDesc)
            response(ApiFactory.apiService.workAssetRoleSave(body)) {

            }
        }
    }
}