package com.kgmedia.fastfilm.feature.creator.assets

import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveRoleBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.assets.edit.ReferenceHistoricalImageryDialog
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ItemRoleVM(
    index: Int,
    workId: String?,
    storyId: String?,
    onItemChanged: () -> Unit,
    data: SimpleRoleInfo?
) : ItemAssetVM<SimpleRoleInfo>(index, workId, storyId, onItemChanged, data) {

    override val assetType: String = "角色"
    override val assetDescHint: String = "角色描述词"
    override val assetNameHint: String = "角色名"
    override val generateButtonText: String = "生成角色"

    override fun clickGenerate() {
        if (status.get() == 1) return
        val rolePrompt = if (editingAssetDesc.get()) editingAssetDescText else assetDesc.get()
        if (rolePrompt.isNullOrEmpty()) {
            "请先填写角色描述".showToast()
            return
        }
        val fragmentManager = (topActivity as? FragmentActivity)?.supportFragmentManager
        if (data.get()?.hasHistory == true && fragmentManager != null) {
            ReferenceHistoricalImageryDialog.newInstance(data.get()?.roleId, storyId, workId, ReferenceHistoricalImageryDialog.BIZ_TYPE_ROLE_IMAGE)
                .apply { onUserReference = { generate(rolePrompt, it) } }
                .show(fragmentManager)
        } else {
            generate(rolePrompt)
        }
    }

    override fun generate(assetDesc: String, reference: ReferenceImageInfo?) {
        val preStatus = status.get()
        if (preStatus == 1) return
        status.set(1)
        onItemChanged()
        launch {
            var role = data.get()
            if (assetDesc != role?.rolePrompt) {
                val response = withContext(Dispatchers.IO) {
                    val body = SaveRoleBody(
                        workId = workId,
                        storyId = storyId,
                        roleId = role?.roleId,
                        appearanceId = role?.appearanceId,
                        rolePrompt = assetDesc,
                        roleName = null,
                    )
                    ApiFactory.apiService.workAssetRoleSave(body)
                }
                if (response.isSuccess && response.data != null) {
                    role = response.data
                    data.set(response.data)
                } else {
                    response.msg?.showToast()
                    status.set(preStatus)
                    onItemChanged()
                    return@launch
                }
            }
            val body = GenerateAssetBody(
                workId = workId,
                storyId = storyId,
                assetId = role?.roleId,
                appearanceId = role?.appearanceId,
                referenceImageId = reference?.id,
                referenceImageUrl = reference?.referenceImageUrl,
                referenceImageInStatus = if (reference != null) 1 else 0,
                bizType = if (reference != null) 42 else 41,
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
                    if (data?.genStatus == 4) PowerRechargeDialog.show(true)
                },
                error = {
                    this?.msg?.showToast()
                    status.set(preStatus)
                    onItemChanged()
                })
        }
    }

    override suspend fun delete(): ApiResponse<Any> {
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
            } else {
                onItemChanged()
            }
        }
    }
}