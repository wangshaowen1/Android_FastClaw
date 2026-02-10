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
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.PhotoPagerActivity
import com.kgmedia.fastfilm.feature.creator.assets.ItemAddVM
import com.kgmedia.fastfilm.widget.GeneratingDialog
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
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val adapter by lazy { StoryboardDesignAdapter() }
    private val addStoryboardItem by lazy { ItemAddVM("添加分镜").apply { onItemClick.value = { addStoryboard(this) } } }
    private var editingStoryboardDescVM: ItemStoryboardVM? = null
    private var loopNextEnabledJob: Job? = null
    private var loopAllStoryboardDescriptionStatusJob: Job? = null
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
                    generatingDialog = topActivity?.let { activity -> GeneratingDialog(activity, "分镜生成中...") }
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
                    editingStoryboardDescVM?.exitEditStoryboardDesc()
                    editingStoryboardDescVM = null
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
        loopAllStoryboardDescriptionStatusJob?.cancel()
        inputJob?.cancel()
        adapter.items.forEach { if (it is ItemStoryboardVM) it.clear() }
        super.onCleared()
    }

    fun retry() {
        when (step) {
            0 -> generateAllStoryboardDescription()
            1 -> loopAllStoryboardDescriptionStatus()
            2 -> getStoryboards()
        }
    }

    private fun loopNextEnabled() {
        loopNextEnabledJob?.cancel()
        loopNextEnabledJob = launch {
            val containStoryboard = adapter.items.any { it is ItemStoryboardVM }
            nextEnabled.set(containStoryboard && adapter.items.all { if (it is ItemStoryboardVM) it.data.get()?.imageGenerated == true else true })
            delay(1000)
            loopNextEnabled()
        }
    }

    private fun generateAllStoryboardDescription() {
        if (step > 0) return
        generating = true
        launch(block = {
            val allStoryboardDescriptionStatus = withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescription(WorkBody(workId, storyId)).data }
            taskId = allStoryboardDescriptionStatus?.taskId
            if (allStoryboardDescriptionStatus?.genStatus == 1 || allStoryboardDescriptionStatus?.genStatus == 2) {
                step = 1
                loopAllStoryboardDescriptionStatus()
            } else {
                loadState.set(LoadStateViewHolder.RETRY)
                generating = false
            }
        }, onError = {
            loadState.set(LoadStateViewHolder.RETRY)
            generating = false
        })
    }

    private fun loopAllStoryboardDescriptionStatus() {
        loopAllStoryboardDescriptionStatusJob?.cancel()
        generating = true
        loopAllStoryboardDescriptionStatusJob = launch(block = {
            var genStatus: Int? = 1
            runCatching {
                val status = withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescriptionStatus(TaskIdBody(taskId, null)).data?.genStatus }
                genStatus = status
            }
            //生成状态 [0=待生成 1=生成中 2=成功 3=失败]
            when (genStatus) {
                1 -> {
                    delay(3000)
                    loopAllStoryboardDescriptionStatus()
                }

                2 -> {
                    step = 2
                    getStoryboards()
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

    fun getStoryboards() {
        generating = true
        launch(
            block = {
                val storyboards = mRepo.api.workStoryboardList(WorkBody(workId, storyId)).data
                val items = mutableListOf<AppItemViewModel<*>>()
                storyboards?.map { it.toItemStoryboardVM() }?.let { items.addAll(it) }
                items.add(addStoryboardItem)
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

    private fun SimpleStoryboardInfo.toItemStoryboardVM() = ItemStoryboardVM(workId, storyId, this).applyConfig()

    fun addStoryboard(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        nextEnabled.set(false)
        adapter.add(index, ItemStoryboardVM(workId, storyId, null).applyConfig())
        updateItemNo()
    }

    private fun updateItemNo() {
        var storyboardNum = 0
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemStoryboardVM) {
                storyboardNum++
                item.no.set(storyboardNum)
            }
        }
    }

    private fun ItemStoryboardVM.applyConfig(): ItemStoryboardVM {
        onItemClick.value = { view ->
            when (view?.id) {
                R.id.btnPreview -> {
                    data.get()?.imageUrl?.let { PhotoPagerActivity.launch(topActivity, it) }
                }

                R.id.etStoryboardDescCover -> {
                    inputJob?.cancel()
                    val edit: (delay: Long) -> Unit = { delay ->
                        editingStoryboardDescVM?.exitEditStoryboardDesc()
                        editingStoryboardDescVM = this
                        if (delay > 0) {
                            inputJob = launch {
                                delay(delay)
                                editStoryboardDesc()
                            }
                        } else {
                            editStoryboardDesc()
                        }
                    }
                    if (genImagePrompt.get().isNullOrEmpty()) {
                        edit.invoke(0)
                    } else {
                        topActivity?.showCommonDialog(tip = "您想要编辑文字内容，修改内容可能影响 AI 视频生成质量.", confirmText = "继续编辑") {
                            edit.invoke(200)
                        }
                    }
                }

                R.id.btnDelete -> {
                    if (data.get() == null) {
                        adapter.remove(this)
                        updateItemNo()
                    } else {
                        topActivity?.showCommonDialog(tip = "确认删除吗？", confirmText = "删除") {
                            deleteStoryboard(this)
                        }
                    }
                }
            }
        }
        return this
    }

    private fun deleteStoryboard(item: ItemStoryboardVM) {
        launchWithLoading {
            response(mRepo.api.workStoryboardDelete(IdsBody(null, item.data.get()?.storyboardId))) {
                adapter.remove(item)
                updateItemNo()
            }
        }
    }

    fun oneKeyGenerate() {
        val notGenerateItems = arrayListOf<ItemStoryboardVM>()
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemStoryboardVM) {
                val genStatusImage = item.data.get()?.genStatusImage ?: 1
                if (genStatusImage < 1) {
                    val genImagePrompt = item.genImagePrompt.get()
                    if (genImagePrompt.isNullOrEmpty()) {
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