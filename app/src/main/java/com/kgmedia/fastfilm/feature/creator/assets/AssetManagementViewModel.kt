package com.kgmedia.fastfilm.feature.creator.assets

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
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.SimplePropInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.core.network.apis.request.StrategyTypeBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.PhotoPagerActivity
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.ext.dp
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
    companion object {
        private const val POWER = "[power]"

        /**
         * 初始化
         */
        private const val STEP_INIT = 0

        /**
         * 资产描述生成中
         */
        private const val STEP_DESCRIPTION_GENERATING = 1

        /**
         * 资产描述已生成
         */
        private const val STEP_DESCRIPTION_GENERATED = 2

        /**
         * 资产图片生成中
         */
        private const val STEP_IMAGE_GENERATING = 3

        /**
         * 资产图片生成失败
         */
        private const val STEP_IMAGE_GENERATE_FAILED = 4

        /**
         * 资产图片已生成
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
    val adapter by lazy { AssetManagementAdapter() }
    private val addRoleItem by lazy { ItemAddVM("添加角色").apply { onItemClick.value = { addRole(this) } } }
    private val addSceneItem by lazy { ItemAddVM("添加场景").apply { onItemClick.value = { addScene(this) } } }
    private val addPropItem by lazy { ItemAddVM("添加道具").apply { onItemClick.value = { addProp(this) } } }
    private var editingAssetDescVM: ItemAssetVM<*>? = null
    private var loopNextEnabledJob: Job? = null
    private var loopAllAssetDescriptionStatusJob: Job? = null
    private var inputJob: Job? = null
    val step by lazy { ObservableInt(STEP_INIT) }
    val cost by lazy { ObservableInt() }
    private var aiPrice = 0
    val keyboardHeight by lazy { ObservableInt(100) }
    private var generatingDialog: GeneratingDialog? = null
    private var generating = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    progressHelper.start("提取资产信息", step = 1, intervalMs = 100L)
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
    private var editConfirm = false

    override fun onResume() {
        super.onResume()
        topActivity?.let {
            KeyboardUtils.registerSoftInputChangedListener(it) { height ->
                keyboardHeight.set(height)
                if (height <= 0) {
                    editingAssetDescVM?.exitEditAssetDesc()
                    editingAssetDescVM = null
                }
            }
        }
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
        when (step.get()) {
            STEP_INIT -> generateAllAssetDescription()
            STEP_DESCRIPTION_GENERATING -> loopAllAssetDescriptionStatus()
            STEP_DESCRIPTION_GENERATED -> getAssets()
        }
    }

    private fun generateAllAssetDescription() {
        if (step.get() > STEP_INIT) return
        generating = true
        launch(block = {
            val allAssetDescriptionStatus = withContext(Dispatchers.IO) { mRepo.api.generateAllAssetDescription(WorkBody(workId, storyId)).data }
            taskId = allAssetDescriptionStatus?.taskId
            if (allAssetDescriptionStatus?.genStatus == 1 || allAssetDescriptionStatus?.genStatus == 2) {
                step.set(STEP_DESCRIPTION_GENERATING)
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
                    step.set(STEP_DESCRIPTION_GENERATED)
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
                roles?.mapIndexed { index, info -> info.toItemAssetVM(index) }?.let { items.addAll(it) }
                items.add(addRoleItem)
                scenes?.mapIndexed { index, info -> info.toItemAssetVM(index) }?.let { items.addAll(it) }
                items.add(addSceneItem)
                props?.mapIndexed { index, info -> info.toItemAssetVM(index) }?.let { items.addAll(it) }
                items.add(addPropItem)
                adapter.submitList(items)
                onItemChanged()
                loadState.set(LoadStateViewHolder.CONTENT)
                generating = false
            },
            onError = {
                loadState.set(LoadStateViewHolder.RETRY)
                generating = false
            }
        )
    }

    private fun SimpleRoleInfo.toItemAssetVM(index: Int) = ItemRoleVM(index, workId, storyId, ::onItemChanged, this).applyConfig()

    private fun SimpleSceneInfo.toItemAssetVM(index: Int) = ItemSceneVM(index, workId, storyId, ::onItemChanged, this).applyConfig()

    private fun SimplePropInfo.toItemAssetVM(index: Int) = ItemPropVM(index, workId, storyId, ::onItemChanged, this).applyConfig()

    fun addRole(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemRoleVM(adapter.items.count { it is ItemRoleVM }, workId, storyId, ::onItemChanged, null).applyConfig())
        onItemChanged()
    }

    fun addScene(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemSceneVM(adapter.items.count { it is ItemSceneVM }, workId, storyId, ::onItemChanged, null).applyConfig())
        onItemChanged()
    }

    fun addProp(addItem: ItemAddVM) {
        val index = adapter.items.indexOf(addItem)
        adapter.add(index, ItemPropVM(adapter.items.count { it is ItemPropVM }, workId, storyId, ::onItemChanged, null).applyConfig())
        onItemChanged()
    }

    private fun onItemChanged() {
        var newNum = 0
        var generatingNum = 0
        var generatedNum = 0
        var failedNum = 0
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemAssetVM<*>) {
                //-1=新创建 0=待生成 1=生成中 2=成功 3=失败
                when (item.status.get()) {
                    -1, 0 -> newNum++
                    1 -> generatingNum++
                    2 -> generatedNum++
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

    private fun ItemAssetVM<*>.applyConfig(): ItemAssetVM<*> {
        onItemClick.value = { view ->
            when (view?.id) {
                R.id.ivCover -> {
                    assetImage.get()?.let { PhotoPagerActivity.launch(topActivity, it) }
                }

                R.id.etAssetDescCover -> {
                    inputJob?.cancel()
                    val edit: (delay: Long) -> Unit = { delay ->
                        editingAssetDescVM?.exitEditAssetDesc()
                        editingAssetDescVM = this
                        if (delay > 0) {
                            inputJob = launch {
                                delay(delay)
                                editAssetDesc()
                            }
                        } else {
                            editAssetDesc()
                        }
                    }
                    if (assetDesc.get().isNullOrEmpty() || editConfirm) {
                        edit.invoke(0)
                    } else {
                        editConfirm = true
                        topActivity?.showCommonDialog(tip = "您想要编辑文字内容，修改内容可能影响 AI 视频生成质量.", confirmText = "继续编辑") {
                            edit.invoke(200)
                        }
                    }
                }

                R.id.btnDelete -> {
                    if (data.get() == null) {
                        adapter.remove(this)
                        onItemChanged()
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
            response(item.delete()) {
                adapter.remove(item)
                onItemChanged()
            }
        }
    }

    private fun oneKeyGenerate() {
        val cost = cost.get()
        if (cost == 0) {
            calcCost()
            return
        }
        if (PowerRechargeDialog.checkLowBalance(cost)) return
        var notInput = false
        val notGenerateItems = arrayListOf<Pair<ItemAssetVM<*>, String>>()
        adapter.items.forEachIndexed { index, item ->
            if (item is ItemAssetVM<*>) {
                if (item.status.get() < 1 || item.status.get() == 3) {
                    val assetDesc = if (item.editingAssetDesc.get()) item.editingAssetDescText else item.assetDesc.get()
                    if (assetDesc.isNullOrEmpty()) {
                        notInput = true
                    } else {
                        notGenerateItems.add(item to assetDesc)
                    }
                }
            }
        }
        if (notInput) {
            "您还有资产尚未填写描述，请补充后再继续".showToast()
        }
        if (notGenerateItems.isNotEmpty()) {
            notGenerateItems.forEach { (item, assetDesc) -> item.generate(assetDesc) }
        }
    }

    private fun calcCost() {
        cost.set(0)
        launch {
            if (aiPrice <= 0) {
                withContext(Dispatchers.IO) {
                    mRepo.api.queryAiPrice(StrategyTypeBody(1)).data?.pointNum?.let { aiPrice = it }
                }
            }
            if (aiPrice > 0) {
                val num = adapter.items.count { it is ItemAssetVM<*> && it.status.get().let { status -> status < 1 || status == 3 } }
                cost.set(aiPrice * num)
            }
        }
    }

    private fun generateAllStoryboardDescription(toNextStep: ((workId: String?, storyId: String?, taskId: String?) -> Unit)?) {
        launchWithLoading {
            response(mRepo.api.generateAllStoryboardDescription(WorkBody(workId, storyId))) {
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
            STEP_IMAGE_GENERATED -> "下一步：生成分镜图片"
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
                generateAllStoryboardDescription(toNextStep)
            }
        }
    }
}