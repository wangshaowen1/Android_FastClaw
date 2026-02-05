package com.kgmedia.fastfilm.feature.creator.assets

import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveRoleBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ItemRoleVM(
    workId: String?,
    storyId: String?,
    data: SimpleRoleInfo?
) : ItemAssetVM<SimpleRoleInfo>(workId, storyId, data) {

    override fun exitEditAssetDesc() {
        super.exitEditAssetDesc()
        saveInput()
    }

    override fun exitEditAssetName() {
        super.exitEditAssetName()
        saveInput()
    }

    private fun saveInput() {
        val rolePrompt = assetDesc.get()?.trim()
        val roleName = assetName.get()?.trim()
        if (rolePrompt.isNullOrEmpty() && roleName.isNullOrEmpty()) {
            return
        }
        launch {
            val body = SaveRoleBody(data.get()?.roleId, data.get()?.appearanceId, workId, storyId, roleName, rolePrompt)
            response(ApiFactory.apiService.workAssetRoleSave(body)) {
                this@ItemRoleVM.data.set(data)
            }
        }
    }

    override fun generate() {
        if (status.get() == 1) return
        val rolePrompt = if (editingAssetDesc.get()) editingAssetDescText else assetDesc.get()
        val roleName = if (editingAssetName.get()) editingAssetNameText else assetName.get()
        if (rolePrompt.isNullOrEmpty()) {
            "请先填写角色描述".showToast()
            return
        }
        if (roleName.isNullOrEmpty()) {
            "请先填写角色名称".showToast()
            return
        }
        status.set(1)
        launch {
            val body = GenerateAssetBody(41, rolePrompt, data.get()?.appearanceId, null, null, null, workId, storyId)
            response(ApiFactory.apiService.workAssetSingleGenerate(body)) {
                val role = this@ItemRoleVM.data.get() ?: SimpleRoleInfo()
                role.taskId = data?.taskId
                role.genStatus = data?.genStatus
                data?.genStatus?.let { status.set(it) }
                this@ItemRoleVM.data.set(role)
            }
        }
    }

    override fun deleteAsset() {
        launch {
            response(ApiFactory.apiService.workAssetRoleDelete(IdsBody(null, data.get()?.roleId))) {
            }
        }
    }

    override fun loopGenStatus() {
        launch {
            loop@ while (status.get() == 1) {
                runCatching {
                    val genStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(data.get()?.taskId, 41)).data?.genStatus }
                    genStatus?.let { status.set(it) }
                    if (genStatus == 2) {
                        val body = QueryAssetBody(
                            data.get()?.roleId,
                            data.get()?.appearanceId,
                            41,
                            storyId,
                            workId
                        )
                        val role = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetRoleList(body).data?.firstOrNull { it.roleId == data.get()?.roleId } }
                        if (role != null) {
                            this@ItemRoleVM.data.set(role)
                            role.genStatus?.let { status.set(it) }
                        }
                    }
                }
                if (status.get() != 1) {
                    break@loop
                }
            }
            delay(3000)
        }
    }
}