package com.kgmedia.fastfilm.feature.creator.storyboard

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import com.blankj.utilcode.util.KeyboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.assets.ItemAddVM
import com.kgmedia.fastfilm.feature.creator.assets.ItemAssetVM
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * 分镜设计
 */
class StoryboardDesignViewModel : AppRepositoryViewModel<AppRepository>() {
    private val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    private val storyId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val loadState by lazy { ObservableInt() }
    val adapter by lazy { StoryboardDesignAdapter() }
    private val addStoryboardItem by lazy { ItemAddVM("添加分镜").apply { onItemClick.value = { addStoryboard(this) } } }
    private var editingStoryboardDescVM: ItemStoryboardVM? = null
    private var loopNextEnabledJob: Job? = null
    val nextEnabled by lazy { ObservableBoolean(false) }

    override fun onCreate() {
        super.onCreate()
        getStoryboards()
    }

    override fun onResume() {
        super.onResume()
        topActivity?.let {
            KeyboardUtils.registerSoftInputChangedListener(it) { height ->
                if (height <= 0) {
                    editingStoryboardDescVM?.exitEditAssetDesc()
                    editingStoryboardDescVM = null
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

    fun getStoryboards() {
        loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val storyboards = mRepo.api.workStoryboardList(WorkBody(workId, storyId)).data
                val items = mutableListOf<AppItemViewModel<*>>()
                storyboards?.map { it.toItemStoryboardVM() }?.let { items.addAll(it) }
                items.add(addStoryboardItem)
                adapter.submitList(items)
                loadState.set(LoadStateViewHolder.CONTENT)
            },
            onError = {
                loadState.set(LoadStateViewHolder.RETRY)
            }
        )
    }

    private fun SimpleStoryboardInfo.toItemStoryboardVM() = ItemStoryboardVM(workId, storyId, this).applyConfig()

    fun addStoryboard(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemStoryboardVM(workId, storyId, null).applyConfig())
    }

    private fun ItemStoryboardVM.applyConfig(): ItemStoryboardVM {
        onItemClick.value = { view ->
            when (view?.id) {
                R.id.btnPreview -> {
                }

                R.id.etStoryboardDescCover -> {
                    topActivity?.showCommonDialog(tip = "您想要编辑文字内容，修改内容可能影响 AI 视频生成质量.", confirmText = "继续编辑") {
                        editingStoryboardDescVM?.exitEditAssetDesc()
                        editingStoryboardDescVM = this
                        editAssetDesc()
                    }
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
        val notGenerateItems = arrayListOf<ItemStoryboardVM>()
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemStoryboardVM) {
                if (item.status.get() < 1) {
                    val assetDesc = item.storyboardDesc.get()
                    if (assetDesc.isNullOrEmpty()) {
                        "您还有分镜尚未填写描述，请补充后再继续".showToast()
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

    fun next() {
        launchWithLoading {
            response(mRepo.api.workStoryboardVideoGenerate(WorkBody(workId, storyId))) {

            }
        }
    }

}