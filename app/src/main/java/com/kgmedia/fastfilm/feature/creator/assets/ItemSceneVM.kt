package com.kgmedia.fastfilm.feature.creator.assets

import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.sensitiveWordCheck
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveSceneBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.assets.edit.ReferenceHistoricalImageryDialog
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ItemSceneVM(
    index: Int,
    workId: String?,
    storyId: String?,
    onItemChanged: () -> Unit,
    data: SimpleSceneInfo?
) : ItemAssetVM<SimpleSceneInfo>(index, workId, storyId, onItemChanged, data) {

    override val assetType: String = "场景"
    override val assetDescHint: String = "场景描述词"
    override val assetNameHint: String = "场景名"
    override val generateButtonText: String = "生成场景"

    override fun clickGenerate() {
        if (status.get() == 1) return
        val scenePrompt = if (editingAssetDesc.get()) editingAssetDescText else assetDesc.get()
        val sceneName = if (editingAssetName.get()) editingAssetNameText else assetName.get()
        if (scenePrompt.isNullOrEmpty()) {
            "请先填写场景描述".showToast()
            return
        }
        val fragmentManager = (topActivity as? FragmentActivity)?.supportFragmentManager
        if (data.get()?.hasHistory == true && fragmentManager != null) {
            ReferenceHistoricalImageryDialog.newInstance(data.get()?.sceneId, storyId, workId, ReferenceHistoricalImageryDialog.BIZ_TYPE_SCENE_IMAGE)
                .apply { onUserReference = { generate(scenePrompt.toString(), sceneName.toString(), it) } }
                .show(fragmentManager)
        } else {
            generate(scenePrompt.toString(), sceneName.toString())
        }
    }

    override fun generate(scenePrompt: String, sceneName: String, reference: ReferenceImageInfo?) {
        val preStatus = status.get()
        if (preStatus == 1) return
        updateStatus(1)
        launch(
            block = {
                var scene = data.get()
                if (scenePrompt != scene?.scenePrompt || sceneName != scene.sceneName) {
                    sensitiveWordCheck(scenePrompt).let { (isPass, highlightText) ->
                        if (!isPass) {
                            highlightText?.let { assetDesc.set(it) }
                            updateStatus(preStatus)
                            return@launch
                        }
                    }
                    sensitiveWordCheck(sceneName).let { (isPass, highlightText) ->
                        if (!isPass) {
                            highlightText?.let { assetName.set(it) }
                            updateStatus(preStatus)
                            return@launch
                        }
                    }
                    val response = withContext(Dispatchers.IO) {
                        val body = SaveSceneBody(
                            workId = workId,
                            storyId = storyId,
                            sceneId = scene?.sceneId,
                            appearanceId = scene?.appearanceId,
                            scenePrompt = scenePrompt,
                            sceneName = sceneName,
                        )
                        ApiFactory.apiService.workAssetSceneSave(body)
                    }
                    if (response.isSuccess && response.data != null) {
                        scene = response.data
                        data.set(response.data)
                    } else {
                        response.msg?.showToast()
                        updateStatus(preStatus)
                        return@launch
                    }
                }
                val body = GenerateAssetBody(
                    workId = workId,
                    storyId = storyId,
                    assetId = scene?.sceneId,
                    appearanceId = scene?.appearanceId,
                    referenceImageId = reference?.id,
                    referenceImageUrl = reference?.referenceImageUrl,
                    referenceImageInStatus = if (reference != null) 1 else 0,
                    bizType = if (reference != null) 62 else 61,
                )
                response(
                    bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                    result = {
                        val scene = this@ItemSceneVM.data.get() ?: SimpleSceneInfo()
                        scene.taskId = data?.taskId
                        scene.genStatus = data?.genStatus
                        data?.genStatus?.let { updateStatus(it) }
                        this@ItemSceneVM.data.set(scene)
                        loopGenStatus()
                    },
                    error = {
                        if (this?.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                            PowerRechargeDialog.show(true)
                        } else {
                            this?.msg?.showToast()
                        }
                        updateStatus(preStatus)
                    })
            },
            onError = { updateStatus(preStatus) }
        )
    }

    override suspend fun save(scenePrompt: String, sceneName: String): Boolean {
        val scene = data.get()
        if (scenePrompt == scene?.scenePrompt && sceneName == scene.sceneName) return true
        if (scenePrompt != scene?.scenePrompt) {
            sensitiveWordCheck(scenePrompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetDesc.set(it) }
                    return false
                }
            }
        }
        if (sceneName != scene?.sceneName) {
            sensitiveWordCheck(sceneName).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { assetName.set(it) }
                    return false
                }
            }
        }
        val response = withContext(Dispatchers.IO) {
            val body = SaveSceneBody(
                workId = workId,
                storyId = storyId,
                sceneId = scene?.sceneId,
                appearanceId = scene?.appearanceId,
                scenePrompt = scenePrompt,
                sceneName = sceneName,
            )
            ApiFactory.apiService.workAssetSceneSave(body)
        }
        if (response.isSuccess && response.data != null) {
            data.set(response.data)
        } else {
            response.msg?.showToast()
            return false
        }
        return true
    }

    override suspend fun delete(): ApiResponse<Any> {
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSceneDelete(IdsBody(null, data.get()?.appearanceId)) }
    }

    override fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
            runCatching {
                val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(taskId, 61)).data }
                data.get()?.let {
                    it.genStatus = taskStatus?.genStatus
                    it.imageUrl = taskStatus?.result
                    assetImage.set(taskStatus?.result)
                }
                data.notifyChange()
                taskStatus?.genStatus?.let { updateStatus(it) }
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