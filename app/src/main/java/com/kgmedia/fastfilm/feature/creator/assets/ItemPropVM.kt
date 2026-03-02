package com.kgmedia.fastfilm.feature.creator.assets

import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.SensitiveHighlighter
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.request.ContentBody
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.SavePropBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.assets.edit.ReferenceHistoricalImageryDialog
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ItemPropVM(
    index: Int,
    workId: String?,
    storyId: String?,
    onItemChanged: () -> Unit,
    data: SimplePropInfo?
) : ItemAssetVM<SimplePropInfo>(index, workId, storyId, onItemChanged, data) {

    override val assetType: String = "道具"
    override val assetDescHint: String = "道具描述词"
    override val assetNameHint: String = "道具名"
    override val generateButtonText: String = "生成道具"

    override fun clickGenerate() {
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
        val fragmentManager = (topActivity as? FragmentActivity)?.supportFragmentManager
        if (data.get()?.hasHistory == true && fragmentManager != null) {
            ReferenceHistoricalImageryDialog.newInstance(data.get()?.propId, storyId, workId, ReferenceHistoricalImageryDialog.BIZ_TYPE_PROP_IMAGE)
                .apply { onUserReference = { generate(propPrompt.toString(), propName.toString(), it) } }
                .show(fragmentManager)
        } else {
            generate(propPrompt.toString(), propName.toString())
        }
    }

    override fun generate(propPrompt: String, propName: String, reference: ReferenceImageInfo?) {
        val preStatus = status.get()
        if (preStatus == 1) return
        status.set(1)
        onItemChanged()
        launch {
            var prop = data.get()
            if (propPrompt != prop?.propPrompt || propName != prop.propName) {
                val checkPromptRes = withContext(Dispatchers.IO) { ApiFactory.apiService.sensitiveWordCheck(ContentBody(propPrompt)) }
                val checkPromptData = checkPromptRes.data
                if (checkPromptRes.isSuccess && checkPromptData != null) {
                    if (checkPromptData.isSensitive()) {
                        checkPromptData.content?.showToast()
                        val sp = SensitiveHighlighter.highlight(
                            text = propPrompt,
                            keywords = checkPromptData.hitWords,
                            color = R.color.func_failure_dark.getResColor()
                        )
                        assetDesc.set(sp)
                        status.set(preStatus)
                        onItemChanged()
                        return@launch
                    }
                } else {
                    (checkPromptRes.msg ?: "敏感词校验失败").showToast()
                    status.set(preStatus)
                    onItemChanged()
                    return@launch
                }
                val checkNameRes = withContext(Dispatchers.IO) { ApiFactory.apiService.sensitiveWordCheck(ContentBody(propName)) }
                val checkNameData = checkNameRes.data
                if (checkNameRes.isSuccess && checkNameData != null) {
                    if (checkNameData.isSensitive()) {
                        checkNameData.content?.showToast()
                        val sp = SensitiveHighlighter.highlight(
                            text = propName,
                            keywords = checkNameData.hitWords,
                            color = R.color.func_failure_dark.getResColor()
                        )
                        assetName.set(sp)
                        status.set(preStatus)
                        onItemChanged()
                        return@launch
                    }
                } else {
                    (checkNameRes.msg ?: "敏感词校验失败").showToast()
                    status.set(preStatus)
                    onItemChanged()
                    return@launch
                }
                val response = withContext(Dispatchers.IO) {
                    val body = SavePropBody(
                        workId = workId,
                        storyId = storyId,
                        propId = prop?.propId,
                        appearanceId = prop?.appearanceId,
                        propPrompt = propPrompt,
                        propName = propName,
                    )
                    ApiFactory.apiService.workAssetPropSave(body)
                }
                if (response.isSuccess && response.data != null) {
                    prop = response.data
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
                assetId = prop?.propId,
                appearanceId = prop?.appearanceId,
                referenceImageId = reference?.id,
                referenceImageUrl = reference?.referenceImageUrl,
                referenceImageInStatus = if (reference != null) 1 else 0,
                bizType = if (reference != null) 52 else 51,
            )
            response(
                bean = ApiFactory.apiService.workAssetSingleGenerate(body),
                result = {
                    val prop = this@ItemPropVM.data.get() ?: SimplePropInfo()
                    prop.taskId = data?.taskId
                    prop.genStatus = data?.genStatus
                    data?.genStatus?.let { status.set(it) }
                    this@ItemPropVM.data.set(prop)
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
        return withContext(Dispatchers.IO) { ApiFactory.apiService.workAssetPropDelete(IdsBody(null, data.get()?.appearanceId)) }
    }

    override fun loopGenStatus() {
        loopGenStatusJob?.cancel()
        val taskId = data.get()?.taskId
        if (taskId.isNullOrEmpty()) return
        loopGenStatusJob = launch {
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
            if (status.get() == 1) {
                delay(3000)
                loopGenStatus()
            } else {
                onItemChanged()
            }
        }
    }
}
