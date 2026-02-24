package com.kgmedia.fastfilm.feature.creator.storyboard

import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.KeyboardUtils
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.CenterImageSpan
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.AutoProgressSimulator
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
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 分镜设计
 */
class StoryboardDesignViewModel : AppRepositoryViewModel<AppRepository>() {
    companion object {
        private const val POWER = "[power]"

        /**
         * 初始化
         */
        private const val STEP_INIT = 0

        /**
         * 分镜描述生成中
         */
        private const val STEP_DESCRIPTION_GENERATING = 1

        /**
         * 分镜描述已生成
         */
        private const val STEP_DESCRIPTION_GENERATED = 2

        /**
         * 分镜图片生成中
         */
        private const val STEP_IMAGE_GENERATING = 3

        /**
         * 分镜图片生成失败
         */
        private const val STEP_IMAGE_GENERATE_FAILED = 4

        /**
         * 分镜图片已生成
         */
        private const val STEP_IMAGE_GENERATED = 5
    }

    var workId: String? = null
    var storyId: String? = null
    var taskId: String? = null
        set(value) {
            field = value
            if (!value.isNullOrEmpty()) {
                step.set(STEP_DESCRIPTION_GENERATING)
            }
        }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val adapter by lazy { StoryboardDesignAdapter() }
    private val addStoryboardItem by lazy { ItemAddVM("添加分镜").apply { onItemClick.value = { addStoryboard(this) } } }
    private var editingStoryboardDescVM: ItemStoryboardVM? = null
    private var loopAllStoryboardDescriptionStatusJob: Job? = null
    private var inputJob: Job? = null

    /**
     * 0 初始化 1 已提交生成所有资产描述 2 所以资产描述已生成
     */
    val step by lazy { ObservableInt(STEP_INIT) }
    val cost by lazy { ObservableInt() }
    val keyboardHeight by lazy { ObservableInt() }
    private var generatingDialog: GeneratingDialog? = null
    private var generating = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    progressHelper.start("提取分镜信息", step = 1, intervalMs = 100L)
                } else {
                    progressHelper.stop()
                }
            }
        }
    private val progressHelper by lazy {
        AutoProgressSimulator(viewModelScope) { show, title, progress ->
            if (show) {
                val dialog = generatingDialog
                if (dialog == null || !dialog.isShowing) {
                    // 只在 Dialog 不存在时创建
                    generatingDialog = topActivity?.let { activity -> GeneratingDialog(activity, "$title $progress%", "退出后AI会继续生成") }
                    generatingDialog?.show()
                } else {
                    // 如果已经在显示，只更新文字内容，不重置 Dialog
                    dialog.updateContent("$title $progress%", "退出后AI会继续生成")
                }
            } else {
                generatingDialog?.dismiss()
                generatingDialog = null
            }
        }
    }
    private val powerSpan by lazy { CenterImageSpan(App.instance, R.drawable.icon_power).apply { setDrawableSize(16.dp, 16.dp) } }

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
    }

    override fun onPause() {
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
        when (step.get()) {
            STEP_INIT -> generateAllStoryboardDescription()
            STEP_DESCRIPTION_GENERATING -> loopAllStoryboardDescriptionStatus()
            STEP_DESCRIPTION_GENERATED -> getStoryboards()
        }
    }

    private fun generateAllStoryboardDescription() {
        if (step.get() > STEP_INIT) return
        generating = true
        launch(block = {
            val allStoryboardDescriptionStatus = withContext(Dispatchers.IO) { mRepo.api.generateAllStoryboardDescription(WorkBody(workId, storyId)).data }
            taskId = allStoryboardDescriptionStatus?.taskId
            if (allStoryboardDescriptionStatus?.genStatus == 1 || allStoryboardDescriptionStatus?.genStatus == 2) {
                step.set(STEP_DESCRIPTION_GENERATING)
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
                    step.set(STEP_DESCRIPTION_GENERATED)
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

    private fun SimpleStoryboardInfo.toItemStoryboardVM() = ItemStoryboardVM(workId, storyId, ::onItemChanged, this).applyConfig()

    fun addStoryboard(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemStoryboardVM(workId, storyId, ::onItemChanged, SimpleStoryboardInfo(genStatusImage = 0)).applyConfig())
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
        onItemChanged()
    }

    private fun onItemChanged() {
        var newNum = 0
        var generatingNum = 0
        var generatedNum = 0
        var failedNum = 0
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemStoryboardVM) {
                //图片生成状态 0=待生成 1=生成中 2=成功 3=失败 99=无需生成
                when (item.data.get()?.genStatusImage) {
                    0 -> newNum++
                    1 -> generatingNum++
                    2, 99 -> generatedNum++
                    3 -> failedNum++
                }
            }
        }
        when {
            generatingNum > 0 -> step.set(STEP_IMAGE_GENERATING)
            newNum > 0 -> {
                step.set(STEP_DESCRIPTION_GENERATED)
                calcCost()
            }

            failedNum > 0 -> {
                step.set(STEP_IMAGE_GENERATE_FAILED)
                calcCost()
            }

            else -> step.set(STEP_IMAGE_GENERATED)
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

    private fun oneKeyGenerate() {
        var notInput = false
        val notGenerateItems = arrayListOf<Pair<ItemStoryboardVM, String>>()
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemStoryboardVM) {
                val genStatusImage = item.data.get()?.genStatusImage ?: 1
                if (genStatusImage < 1 || genStatusImage == 3) {
                    val genImagePrompt = if (item.editingGenImagePrompt.get()) item.editingGenImagePromptText else item.genImagePrompt.get()
                    if (genImagePrompt.isNullOrEmpty()) {
                        notInput = true
                    } else {
                        notGenerateItems.add(item to genImagePrompt)
                    }
                }
            }
        }
        if (notInput) {
            "您还有分镜尚未填写描述，请补充后再继续".showToast()
        }
        if (notGenerateItems.isNotEmpty()) {
            notGenerateItems.forEach { (item, genImagePrompt) -> item.generate(genImagePrompt) }
        }
    }

    private fun calcCost() {
        cost.set((Math.random() * 1000).toInt())
    }

    private fun generateStoryboardVideo(toNextStep: ((workId: String?, storyId: String?, taskId: String?) -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.api.workStoryboardVideoGenerate(WorkBody(workId, storyId))) {
                toNextStep?.invoke(workId, storyId, data?.taskId)
            }
        }
    }

    fun buttonEnable(step: Int): Boolean {
        return step == STEP_DESCRIPTION_GENERATED || step == STEP_IMAGE_GENERATE_FAILED || step == STEP_IMAGE_GENERATED
    }

    fun buttonText(step: Int, cost: Int): CharSequence? {
        return when (step) {
            STEP_DESCRIPTION_GENERATED -> if (cost > 0) "一键生成  $POWER $cost".replaceSpan(POWER) { powerSpan } else "一键生成"
            STEP_IMAGE_GENERATING -> "图片生成中..."
            STEP_IMAGE_GENERATE_FAILED -> if (cost > 0) "重试失败项  $POWER $cost".replaceSpan(POWER) { powerSpan } else "重试失败项"
            STEP_IMAGE_GENERATED -> "生成分镜视频"
            else -> null
        }
    }

    /**
     * 点击下一步
     */
    fun clickNext(toNextStep: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?) {
        when (step.get()) {
            STEP_DESCRIPTION_GENERATED, STEP_IMAGE_GENERATE_FAILED -> {
                //一键生成、重试失败项
                oneKeyGenerate()
            }

            STEP_IMAGE_GENERATED -> {
                //下一步：生成分镜图片
                generateStoryboardVideo(toNextStep)
            }
        }
    }
}