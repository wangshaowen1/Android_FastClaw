package com.kgmedia.fastfilm.feature.creator.assets

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import com.blankj.utilcode.util.KeyboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.PhotoPagerActivity
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 资产管理
 */
class AssetManagementViewModel : AppRepositoryViewModel<AssetManagementRepository>() {
    var workId: String? = null
    var storyId: String? = null
    var taskId: String? = null
        set(value) {
            field = value
            if (!value.isNullOrEmpty()) {
                step = 1
            }
        }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val adapter by lazy { AssetManagementAdapter() }
    private val addRoleItem by lazy { ItemAddVM("添加角色").apply { onItemClick.value = { addRole(this) } } }
    private val addSceneItem by lazy { ItemAddVM("添加场景").apply { onItemClick.value = { addScene(this) } } }
    private val addPropItem by lazy { ItemAddVM("添加道具").apply { onItemClick.value = { addProp(this) } } }
    private var editingAssetDescVM: ItemAssetVM<*>? = null
    private var editingAssetNameVM: ItemAssetVM<*>? = null
    private var loopNextEnabledJob: Job? = null
    private var loopAllAssetDescriptionStatusJob: Job? = null
    private var inputJob: Job? = null

    /**
     * 0 初始化 1 已提交生成所有资产描述 2 所以资产描述已生成
     */
    private var step = 0
    val nextEnabled by lazy { ObservableBoolean(false) }
    val keyboardHeight by lazy { ObservableInt() }
    private var generatingDialog: GeneratingDialog? = null
    private var generating = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    generatingDialog?.dismiss()
                    generatingDialog = topActivity?.let { activity -> GeneratingDialog(activity, "资产生成中...") }
                    generatingDialog?.show()
                } else {
                    generatingDialog?.dismiss()
                }
            }
        }

    override fun onResume() {
        super.onResume()
        topActivity?.let {
            KeyboardUtils.registerSoftInputChangedListener(it) { height ->
                keyboardHeight.set(height)
                if (height <= 0) {
                    editingAssetDescVM?.exitEditAssetDesc()
                    editingAssetNameVM?.exitEditAssetName()
                    editingAssetDescVM = null
                    editingAssetNameVM = null
                }
            }
        }
        loopNextEnabled()
    }

    override fun onPause() {
        loopNextEnabledJob?.cancel()
        topActivity?.window?.let { KeyboardUtils.unregisterSoftInputChangedListener(it) }
        super.onPause()
    }

    override fun onCleared() {
        loopAllAssetDescriptionStatusJob?.cancel()
        inputJob?.cancel()
        adapter.items.forEach { if (it is ItemAssetVM<*>) it.clear() }
        super.onCleared()
    }

    fun retry() {
        when (step) {
            0 -> generateAllAssetDescription()
            1 -> loopAllAssetDescriptionStatus()
            2 -> getAssets()
        }
    }

    private fun loopNextEnabled() {
        loopNextEnabledJob?.cancel()
        loopNextEnabledJob = launch {
            val containAssets = adapter.items.any { it is ItemAssetVM<*> }
            nextEnabled.set(containAssets && adapter.items.all { if (it is ItemAssetVM<*>) it.status.get() == 2 else true })
            delay(1000)
            loopNextEnabled()
        }
    }

    private fun generateAllAssetDescription() {
        if (step > 0) return
        generating = true
        launch(block = {
            val allAssetDescriptionStatus = withContext(Dispatchers.IO) { mRepo.api.generateAllAssetDescription(WorkBody(workId, storyId)).data }
            taskId = allAssetDescriptionStatus?.taskId
            if (allAssetDescriptionStatus?.genStatus == 1 || allAssetDescriptionStatus?.genStatus == 2) {
                step = 1
                loopAllAssetDescriptionStatus()
            } else {
                loadState.set(LoadStateViewHolder.RETRY)
                generating = false
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
            generating = false
        })
    }

    private fun loopAllAssetDescriptionStatus() {
        loopAllAssetDescriptionStatusJob?.cancel()
        generating = true
        loopAllAssetDescriptionStatusJob = launch(block = {
            var genStatus: Int? = 1
            runCatching {
                val status = withContext(Dispatchers.IO) { mRepo.workAssetAllStatus(taskId, null).data?.genStatus }
                genStatus = status
            }
            //生成状态 [0=待生成 1=生成中 2=成功 3=失败]
            when (genStatus) {
                1 -> {
                    delay(3000)
                    loopAllAssetDescriptionStatus()
                }

                2 -> {
                    step = 2
                    getAssets()
                }

                else -> {
                    loadState.set(LoadStateViewHolder.RETRY)
                    generating = false
                }
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
            generating = false
        })
    }

    fun getAssets() {
        generating = true
        launch(
            block = {
                val rolesDeferred = async { mRepo.workAssetRoleList(null, null, null, storyId, workId).data }
                val scenesDeferred = async { mRepo.workAssetSceneList(null, null, null, storyId, workId).data }
                val propsDeferred = async { mRepo.workAssetPropList(null, null, null, storyId, workId).data }
                val roles = rolesDeferred.await()
                val scenes = scenesDeferred.await()
                val props = propsDeferred.await()
                val items = mutableListOf<AppItemViewModel<*>>()
                roles?.map { it.toItemAssetVM() }?.let { items.addAll(it) }
                items.add(addRoleItem)
                scenes?.map { it.toItemAssetVM() }?.let { items.addAll(it) }
                items.add(addSceneItem)
                props?.map { it.toItemAssetVM() }?.let { items.addAll(it) }
                items.add(addPropItem)
                adapter.submitList(items)
                updateItemNo()
                loadState.set(LoadStateViewHolder.CONTENT)
                generating = false
            },
            onError = {
                loadState.set(LoadStateViewHolder.RETRY)
                generating = false
            }
        )
    }

    private fun SimpleRoleInfo.toItemAssetVM() = ItemRoleVM(workId, storyId, this).applyConfig()

    private fun SimpleSceneInfo.toItemAssetVM() = ItemSceneVM(workId, storyId, this).applyConfig()

    private fun SimplePropInfo.toItemAssetVM() = ItemPropVM(workId, storyId, this).applyConfig()

    fun addRole(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        nextEnabled.set(false)
        adapter.add(index, ItemRoleVM(workId, storyId, null).applyConfig())
        updateItemNo()
    }

    fun addScene(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        nextEnabled.set(false)
        adapter.add(index, ItemSceneVM(workId, storyId, null).applyConfig())
        updateItemNo()
    }

    fun addProp(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        nextEnabled.set(false)
        adapter.add(index, ItemPropVM(workId, storyId, null).applyConfig())
        updateItemNo()
    }

    private fun updateItemNo() {
//        var roleNum = 0
//        var sceneNum = 0
//        var propNum = 0
//        adapter.items.forEachIndexed { index, item ->
//            if (item is ItemRoleVM) {
//                roleNum++
//                item.no.set(roleNum)
//            } else if (item is ItemSceneVM) {
//                sceneNum++
//                item.no.set(sceneNum)
//            } else if (item is ItemPropVM) {
//                propNum++
//                item.no.set(propNum)
//            }
//        }
    }

    private fun ItemAssetVM<*>.applyConfig(): ItemAssetVM<*> {
        onItemClick.value = { view ->
            when (view?.id) {
                R.id.btnPreview -> {
                    assetImage.get()?.let { PhotoPagerActivity.launch(topActivity, it) }
                }

                R.id.etAssetDescCover -> {
                    inputJob?.cancel()
                    val edit: (delay: Long) -> Unit = { delay ->
                        editingAssetDescVM?.exitEditAssetDesc()
                        editingAssetNameVM?.exitEditAssetName()
                        editingAssetDescVM = this
                        editingAssetNameVM = null
                        if (delay > 0) {
                            inputJob = launch {
                                delay(delay)
                                editAssetDesc()
                            }
                        } else {
                            editAssetDesc()
                        }
                    }
                    if (assetDesc.get().isNullOrEmpty()) {
                        edit.invoke(0)
                    } else {
                        topActivity?.showCommonDialog(tip = "您想要编辑文字内容，修改内容可能影响 AI 视频生成质量.", confirmText = "继续编辑") {
                            edit.invoke(200)
                        }
                    }
                }

                R.id.etAssetNameCover -> {
                    editingAssetDescVM?.exitEditAssetDesc()
                    editingAssetNameVM?.exitEditAssetName()
                    editingAssetDescVM = null
                    editingAssetNameVM = this
                    editAssetName()
                }

                R.id.btnDelete -> {
                    if (data.get() == null) {
                        adapter.remove(this)
                        updateItemNo()
                    } else {
                        topActivity?.showCommonDialog(tip = "确认删除吗？", confirmText = "删除") {
                            deleteAsset(this)
                        }
                    }
                }
            }
        }
        return this
    }

    private fun deleteAsset(item: ItemAssetVM<*>) {
        launchWithLoading {
            response(item.deleteAsset()) {
                adapter.remove(item)
                updateItemNo()
            }
        }
    }

    fun oneKeyGenerate() {
        val notGenerateItems = arrayListOf<ItemAssetVM<*>>()
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemAssetVM<*>) {
                if (item.status.get() < 1) {
                    val assetDesc = item.assetDesc.get()
                    val assetName = item.assetName.get()
                    if (assetDesc.isNullOrEmpty() || assetName.isNullOrEmpty()) {
                        "您还有资产尚未填写描述，请补充后再继续".showToast()
                        return
                    }
                    notGenerateItems.add(item)
                }
            }
        }
        if (notGenerateItems.isEmpty()) {

        } else {
            notGenerateItems.forEach { it.generate() }
        }
    }

    fun generateAllStoryboardDescription(onSuccess: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?) {
        launchWithLoading {
            response(mRepo.api.generateAllStoryboardDescription(WorkBody(workId, storyId))) {
                onSuccess?.invoke(workId, storyId, data?.taskId)
            }
        }
    }
}