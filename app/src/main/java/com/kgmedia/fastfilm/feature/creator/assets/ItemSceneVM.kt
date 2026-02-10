package com.kgmedia.fastfilm.feature.creator.assets

import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveSceneBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.generate.edit.ReferenceHistoricalImageryDialog
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
        if (scenePrompt.isNullOrEmpty() || sceneName.isNullOrEmpty()) {
            return
        }
        launch {
            val body = SaveSceneBody(data.get()?.sceneId, data.get()?.appearanceId, workId, storyId, sceneName, scenePrompt)
            response(ApiFactory.apiService.workAssetSceneSave(body)) {
                this@ItemSceneVM.data.set(data)
            }
        }
    }

    override val assetDescHint: String = "场景描述词"
    override val assetNameHint: String = "场景名"
    override val generateButtonText: String = "生成场景"

    override fun generate() {
        val status = status.get()
        if (status == 1) return
        val fragmentManager = (topActivity as? FragmentActivity)?.supportFragmentManager
        if (data.get()?.hasHistory == true && fragmentManager != null) {
            ReferenceHistoricalImageryDialog.newInstance(data.get()?.sceneId, storyId, workId, ReferenceHistoricalImageryDialog.BIZ_TYPE_SCENE_IMAGE)
                .apply { onSuccess = { generateAsset(it) } }
                .show(fragmentManager)
        } else {
            generateAsset()
        }
    }

    override fun generateAsset(reference: ReferenceImageInfo?) {
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
            val body = GenerateAssetBody(
                assetId = data.get()?.sceneId,
                appearanceId = data.get()?.appearanceId,
                bizType = 61,
                prompt = scenePrompt,
                referenceImageId = reference?.id,
                referenceImageInStatus = if (reference != null) 1 else 0,
                referenceImageUrl = reference?.referenceImageUrl,
                storyId = storyId,
                workId = workId,
            )
            response(
                bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                result = {
                    val scene = this@ItemSceneVM.data.get() ?: SimpleSceneInfo()
                    scene.taskId = data?.taskId
                    scene.genStatus = data?.genStatus
                    this@ItemSceneVM.data.set(scene)
                    loopGenStatus()
                },
                error = {
                    this?.msg?.showToast()
                    status.set(0)
                })
        }
    }

    override suspend fun deleteAsset(): ApiResponse<Any> {
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
                taskStatus?.genStatus?.let { status.set(it) }
            }
            if (status.get() == 1) {
                delay(3000)
                loopGenStatus()
            }
        }
    }
}