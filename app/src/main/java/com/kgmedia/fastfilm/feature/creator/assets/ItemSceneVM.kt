package com.kgmedia.fastfilm.feature.creator.assets

import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveSceneBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ItemSceneVM(
    workId: String?,
    storyId: String?,
    data: SimpleSceneInfo?
) : ItemAssetVM<SimpleSceneInfo>(workId, storyId, data) {
    override fun exitEditAssetDesc() {
        super.exitEditAssetDesc()
        saveInput()
    }

    override fun exitEditAssetName() {
        super.exitEditAssetName()
        saveInput()
    }

    private fun saveInput() {
        val scenePrompt = assetDesc.get()?.trim()
        val sceneName = assetName.get()?.trim()
        if (scenePrompt.isNullOrEmpty() && sceneName.isNullOrEmpty()) {
            return
        }
        launch {
            val body = SaveSceneBody(data.get()?.sceneId, data.get()?.appearanceId, workId, storyId, sceneName, scenePrompt)
            response(ApiFactory.apiService.workAssetSceneSave(body)) {
                this@ItemSceneVM.data.set(data)
            }
        }
    }

    override fun generate() {
        if (status.get() == 1) return
        val scenePrompt = if (editingAssetDesc.get()) editingAssetDescText else assetDesc.get()
        val sceneName = if (editingAssetName.get()) editingAssetNameText else assetName.get()
        if (scenePrompt.isNullOrEmpty()) {
            "请先填写场景描述".showToast()
            return
        }
        if (sceneName.isNullOrEmpty()) {
            "请先填写场景名称".showToast()
            return
        }
        status.set(1)
        launch {
            val body = GenerateAssetBody(61, scenePrompt, data.get()?.appearanceId, null, null, null, workId, storyId)
            response(ApiFactory.apiService.workAssetSingleGenerate(body)) {
                val scene = this@ItemSceneVM.data.get() ?: SimpleSceneInfo()
                scene.taskId = data?.taskId
                scene.genStatus = data?.genStatus
                this@ItemSceneVM.data.set(scene)
            }
        }
    }

    override fun deleteAsset() {
        launch {
            response(ApiFactory.apiService.workAssetSceneDelete(IdsBody(null, data.get()?.sceneId))) {
            }
        }
    }

    override fun loopGenStatus() {
        launch {
            loop@ while (status.get() == 1) {
                runCatching {
                    val genStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSingleStatus(TaskIdBody(data.get()?.taskId, 61)).data?.genStatus }
                    genStatus?.let { status.set(it) }
                    if (genStatus == 2) {
                        val body = QueryAssetBody(
                            data.get()?.sceneId,
                            data.get()?.appearanceId,
                            61,
                            storyId,
                            workId
                        )
                        val role = withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetSceneList(body).data?.firstOrNull { it.sceneId == data.get()?.sceneId } }
                        if (role != null) {
                            this@ItemSceneVM.data.set(role)
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