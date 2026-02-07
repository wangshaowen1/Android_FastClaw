package com.kgmedia.fastfilm.feature.creator.assets

import android.os.Handler
import android.os.Looper
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
    val loadState by lazy { ObservableInt() }
    val adapter by lazy { AssetManagementAdapter() }
    private val addRoleItem by lazy { ItemAddVM("添加角色").apply { onItemClick.value = { addRole(this) } } }
    private val addSceneItem by lazy { ItemAddVM("添加场景").apply { onItemClick.value = { addScene(this) } } }
    private val addPropItem by lazy { ItemAddVM("添加道具").apply { onItemClick.value = { addProp(this) } } }
    private var editingAssetDescVM: ItemAssetVM<*>? = null
    private var editingAssetNameVM: ItemAssetVM<*>? = null
    private var loopNextEnabledJob: Job? = null

    /**
     * 0 初始化 1 已提交生成所有资产描述 2 所以资产描述已生成
     */
    private var step = 0
    val nextEnabled by lazy { ObservableBoolean(false) }
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var inputRunnable: Runnable? = null
    val keyboardHeight by lazy { ObservableInt() }

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
        loopNextEnabledJob = launch {
            while (true) {
                val containAssets = adapter.items.any { it is ItemAssetVM<*> }
                nextEnabled.set(containAssets && adapter.items.all { if (it is ItemAssetVM<*>) it.status.get() == 2 else true })
                delay(1000)
            }
        }
    }

    fun retry() {
        when (step) {
            0 -> generateAllAssetDescription()
            1 -> loopAllAssetDescriptionStatus()
            2 -> getAssets()
        }
    }

    override fun onPause() {
        loopNextEnabledJob?.cancel()
        topActivity?.window?.let { KeyboardUtils.unregisterSoftInputChangedListener(it) }
        super.onPause()
    }

    private fun generateAllAssetDescription() {
        if (step > 0) return
        loadState.set(LoadStateViewHolder.LOADING)
        launch(block = {
            val allAssetDescriptionStatus = withContext(Dispatchers.IO) { mRepo.api.generateAllAssetDescription(WorkBody(workId, storyId)).data }
            taskId = allAssetDescriptionStatus?.taskId
            if (allAssetDescriptionStatus?.genStatus == 1 || allAssetDescriptionStatus?.genStatus == 2) {
                step = 1
                loopAllAssetDescriptionStatus()
            } else {
                loadState.set(LoadStateViewHolder.RETRY)
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
        })
    }

    private fun loopAllAssetDescriptionStatus() {
        loadState.set(LoadStateViewHolder.LOADING)
        launch(block = {
            var genStatus: Int? = 1
            while (genStatus == 1) {
                runCatching {
                    val status = withContext(Dispatchers.IO) { mRepo.workAssetAllStatus(taskId, null).data?.genStatus }
                    genStatus = status
                }
                delay(3000)
            }
            //生成状态 [0=待生成 1=生成中 2=成功 3=失败]
            if (genStatus == 2) {
                step = 2
                getAssets()
            } else {
                loadState.set(LoadStateViewHolder.RETRY)
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
        })
    }

    fun getAssets() {
        loadState.set(LoadStateViewHolder.LOADING)
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
                loadState.set(LoadStateViewHolder.CONTENT)
            },
            onError = {
                loadState.set(LoadStateViewHolder.RETRY)
            }
        )
    }

    private fun SimpleRoleInfo.toItemAssetVM() = ItemRoleVM(workId, storyId, this).applyConfig()

    private fun SimpleSceneInfo.toItemAssetVM() = ItemSceneVM(workId, storyId, this).applyConfig()

    private fun SimplePropInfo.toItemAssetVM() = ItemPropVM(workId, storyId, this).applyConfig()

    fun addRole(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemRoleVM(workId, storyId, null).applyConfig())
    }

    fun addScene(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemSceneVM(workId, storyId, null).applyConfig())
    }

    fun addProp(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemPropVM(workId, storyId, null).applyConfig())
    }

    private fun ItemAssetVM<*>.applyConfig(): ItemAssetVM<*> {
        onItemClick.value = { view ->
            when (view?.id) {
                R.id.btnPreview -> {
                }

                R.id.etAssetDescCover -> {
                    inputRunnable?.let { handler.removeCallbacks(it) }
                    val edit: (delay: Long) -> Unit = { delay ->
                        editingAssetDescVM?.exitEditAssetDesc()
                        editingAssetNameVM?.exitEditAssetName()
                        editingAssetDescVM = this
                        editingAssetNameVM = null
                        if (delay > 0) {
                            inputRunnable = Runnable { editAssetDesc() }.also {
                                handler.postDelayed(it, delay)
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
                    topActivity?.showCommonDialog(tip = "确认删除吗？", confirmText = "删除") {
                        deleteAsset(this)
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