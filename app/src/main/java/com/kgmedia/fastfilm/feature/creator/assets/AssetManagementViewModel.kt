package com.kgmedia.fastfilm.feature.creator.assets

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import com.blankj.utilcode.util.KeyboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

/**
 * 资产管理
 */
class AssetManagementViewModel : AppRepositoryViewModel<AssetManagementRepository>() {
    private val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    private val storyId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val loadState by lazy { ObservableInt() }
    val adapter by lazy { AssetManagementAdapter() }
    private val addRoleItem by lazy { ItemAddVM("添加角色").apply { onItemClick.value = { addRole(this) } } }
    private val addSceneItem by lazy { ItemAddVM("添加场景").apply { onItemClick.value = { addScene(this) } } }
    private val addPropItem by lazy { ItemAddVM("添加道具").apply { onItemClick.value = { addProp(this) } } }
    private var editingAssetDescVM: ItemAssetVM<*>? = null
    private var editingAssetNameVM: ItemAssetVM<*>? = null
    private var loopNextEnabledJob: Job? = null
    val nextEnabled by lazy { ObservableBoolean(false) }

    override fun onCreate() {
        super.onCreate()
        getAssets()
    }

    override fun onResume() {
        super.onResume()
        topActivity?.let {
            KeyboardUtils.registerSoftInputChangedListener(it) { height ->
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
                nextEnabled.set(adapter.items.all { if (it is ItemAssetVM<*>) it.status.get() == 2 else true })
                delay(1000)
            }
        }
    }

    override fun onPause() {
        loopNextEnabledJob?.cancel()
        topActivity?.window?.let { KeyboardUtils.unregisterSoftInputChangedListener(it) }
        super.onPause()
    }

    fun getAssets() {
        loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val rolesDeferred = async { mRepo.workAssetRoleList(null, null, null, null, null).data }
                val scenesDeferred = async { mRepo.workAssetSceneList(null, null, null, null, null).data }
                val propsDeferred = async { mRepo.workAssetPropList(null, null, null, null, null).data }
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
                    topActivity?.showCommonDialog(tip = "您想要编辑文字内容，修改内容可能影响 AI 视频生成质量.", confirmText = "继续编辑") {
                        editingAssetDescVM?.exitEditAssetDesc()
                        editingAssetNameVM?.exitEditAssetName()
                        editingAssetDescVM = this
                        editingAssetNameVM = null
                        editAssetDesc()
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
                        deleteAsset()
                        adapter.remove(this)
                    }
                }
            }
        }
        return this
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

}