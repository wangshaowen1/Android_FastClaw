package com.kgmedia.fastfilm.feature.creator.assets

import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
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
        if (rolePrompt.isNullOrEmpty() || roleName.isNullOrEmpty()) {
            return
        }
        launch {
            val body = SaveRoleBody(data.get()?.roleId, data.get()?.appearanceId, workId, storyId, roleName, rolePrompt)
            response(ApiFactory.apiService.workAssetRoleSave(body)) {
                this@ItemRoleVM.data.set(data)
            }
        }
    }

    override val assetDescHint: String = "角色描述词"
    override val assetNameHint: String = "角色名"
    override val generateButtonText: String = "生成角色"

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
            val body = GenerateAssetBody(
                assetId = data.get()?.roleId,
                appearanceId = data.get()?.appearanceId,
                bizType = 41,
                prompt = rolePrompt,
                referenceImageId = null,
                referenceImageInStatus = 0,
                referenceImageUrl = null,
                storyId = storyId,
                workId = workId,
            )
            response(
                bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                result = {
                    val role = this@ItemRoleVM.data.get() ?: SimpleRoleInfo()
                    role.taskId = data?.taskId
                    role.genStatus = data?.genStatus
                    data?.genStatus?.let { status.set(it) }
                    this@ItemRoleVM.data.set(role)
                    loopGenStatus()
                },
                error = {
                    this?.msg?.showToast()
                    status.set(0)
                })
        }
    }

    override suspend fun deleteAsset(): ApiResponse<Any> {
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetRoleDelete(IdsBody(null, data.get()?.appearanceId)) }
    }

    override fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, 41)).data }
                data.get()?.let {
                    it.genStatus = taskStatus?.genStatus
                    it.imageUrl = taskStatus?.result
                    assetImage.set(taskStatus?.result)
                }
                data.notifyChange()
                taskStatus?.genStatus?.let { status.set(it) }
            }
            if (status.get() == 1) {
                delay(3000)
                loopGenStatus()
            }
        }
    }
}