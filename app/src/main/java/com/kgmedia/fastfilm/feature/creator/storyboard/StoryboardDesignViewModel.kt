package com.kgmedia.fastfilm.feature.creator.storyboard

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import com.blankj.utilcode.util.KeyboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.assets.ItemAddVM
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 分镜设计
 */
class StoryboardDesignViewModel : AppRepositoryViewModel<AppRepository>() {
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
    val adapter by lazy { StoryboardDesignAdapter() }
    private val addStoryboardItem by lazy { ItemAddVM("添加分镜").apply { onItemClick.value = { addStoryboard(this) } } }
    private var editingStoryboardDescVM: ItemStoryboardVM? = null
    private var loopNextEnabledJob: Job? = null

    /**
     * 0 初始化 1 已提交生成所有资产描述 2 所以资产描述已生成
     */
    private var step = 0
    val nextEnabled by lazy { ObservableBoolean(false) }

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
                val containStoryboard = adapter.items.any { it is ItemStoryboardVM }
                nextEnabled.set(containStoryboard && adapter.items.all { if (it is ItemStoryboardVM) it.data.get()?.imageGenerated == true else true })
                delay(1000)
            }
        }
    }

    fun retry() {
        when (step) {
            0 -> generateAllStoryboardDescription()
            1 -> loopAllStoryboardDescriptionStatus()
            2 -> getStoryboards()
        }
    }

    override fun onPause() {
        loopNextEnabledJob?.cancel()
        topActivity?.window?.let { KeyboardUtils.unregisterSoftInputChangedListener(it) }
        super.onPause()
    }

    private fun generateAllStoryboardDescription() {
        loadState.set(LoadStateViewHolder.LOADING)
        launch(block = {
            val allStoryboardDescriptionStatus = withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescription(WorkBody(workId, storyId)).data }
            taskId = allStoryboardDescriptionStatus?.taskId
            if (allStoryboardDescriptionStatus?.genStatus == 1 || allStoryboardDescriptionStatus?.genStatus == 2) {
                step = 1
                loopAllStoryboardDescriptionStatus()
            } else {
                loadState.set(LoadStateViewHolder.RETRY)
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
        })
    }

    private fun loopAllStoryboardDescriptionStatus() {
        loadState.set(LoadStateViewHolder.LOADING)
        launch(block = {
            var genStatus: Int? = 1
            while (genStatus == 1) {
                runCatching {
                    val status = withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescriptionStatus(TaskIdBody(taskId, null)).data?.genStatus }
                    genStatus = status
                }
                delay(3000)
            }
            //生成状态 [0=待生成 1=生成中 2=成功 3=失败]
            if (genStatus == 2) {
                step = 2
                getStoryboards()
            } else {
                loadState.set(LoadStateViewHolder.RETRY)
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
        })
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
                    val edit = {
                        editingStoryboardDescVM?.exitEditAssetDesc()
                        editingStoryboardDescVM = this
                        editAssetDesc()
                    }
                    if (storyboardDesc.get().isNullOrEmpty()) {
                        edit.invoke()
                    } else {
                        topActivity?.showCommonDialog(tip = "您想要编辑文字内容，修改内容可能影响 AI 视频生成质量.", confirmText = "继续编辑") {
                            edit.invoke()
                        }
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

    fun confirmGenerate(onSuccess: ((workId: String?, storyId: String?, taskId: String?) -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.api.workStoryboardVideoGenerate(WorkBody(workId, storyId))) {
                onSuccess?.invoke(workId, storyId, data?.taskId)
            }
        }
    }

}