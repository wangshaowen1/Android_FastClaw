package com.kgmedia.fastfilm.feature.creator.assets

import androidx.databinding.ObservableInt
import com.blankj.utilcode.util.KeyboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import kotlinx.coroutines.async

/**
 * 资产管理
 */
class AssetManagementViewModel : AppRepositoryViewModel<AssetManagementRepository>() {
    val loadState by lazy { ObservableInt() }
    val adapter by lazy { AssetManagementAdapter() }
    private val addRoleItem by lazy { ItemAddVM("添加角色").apply { onItemClick.value = { addRole(this) } } }
    private val addSceneItem by lazy { ItemAddVM("添加场景").apply { onItemClick.value = { addScene(this) } } }
    private val addPropItem by lazy { ItemAddVM("添加道具").apply { onItemClick.value = { addProp(this) } } }
    private var editingAssetDescVM: ItemAssetVM<*>? = null
    private var editingAssetNameVM: ItemAssetVM<*>? = null
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
    }

    override fun onPause() {
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

    private fun SimpleRoleInfo.toItemAssetVM() = ItemRoleVM(this).applyConfig()

    private fun SimpleSceneInfo.toItemAssetVM() = ItemSceneVM(this).applyConfig()

    private fun SimplePropInfo.toItemAssetVM() = ItemPropVM(this).applyConfig()

    fun addRole(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemRoleVM(null, true).applyConfig())
    }

    fun addScene(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemSceneVM(null, true).applyConfig())
    }

    fun addProp(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemPropVM(null, true).applyConfig())
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
                }
            }
        }
        return this
    }

}