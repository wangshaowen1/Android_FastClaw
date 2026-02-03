package com.kgmedia.fastfilm.feature.editor.regenerate

import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.MultiTypeAdapter
import com.kgmedia.fastfilm.common.adapter.ViewType
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentInfo
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorRepository
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorViewModel
import com.kgmedia.fastfilm.feature.editor.chooserole.ChooseRoleDialog
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast

/**
 * 重绘分镜
 */
class StoryboardRegenerateViewModel : AppRepositoryViewModel<ShortDramaEditorRepository>() {
    private val dramasId by lazy { mBundle.getString(Constants.Extra.DRAMAS_ID) }
    val segment by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, ShortDramaSegmentInfo::class.java) }
    val content by lazy { MutableLiveData(segment?.shotImageDesc) }
    var parentVM: ShortDramaEditorViewModel? = null
        set(value) {
            field = value
            updateAddItem()
        }
    private val addItem by lazy {
        ItemStoryboardRoleVM(null, true).apply {
            onItemClick.value = {
                EventManager.onAppTrack(AppEvent.JG_CLICK_WRITE_ADD_ROLE_BUTTON)
                addRole()
            }
        }
    }
    val adapter by lazy {
        val roles = segment?.roleList?.map { it.toItemStoryboardRoleVM() } ?: emptyList()
        MultiTypeAdapter(object : ViewType<ItemStoryboardRoleVM> {
            override fun getViewType(position: Int, item: ItemStoryboardRoleVM?): Int {
                return if (item?.addRole == true) 1 else 0
            }

            override fun getLayoutId(viewType: Int): Int {
                return if (viewType == 1) R.layout.item_storyboard_role_add else R.layout.item_storyboard_role
            }
        }).also {
            it.submitList(roles)
        }
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    fun optimize() {
        val content = content.value
        if (content.isNullOrEmpty()) {
            "请输入画面描述".showToast()
            return
        }
        promptGenerating.value = true to "正在优化中..."
        launch(block = {
            val promptResponse = mRepo.shortDramasSegmentImageDescOptimize(segment?.shotSegmentId, content)
            promptGenerating.value = false to ""
            if (promptResponse.isSuccess) {
                this@StoryboardRegenerateViewModel.content.value = promptResponse.data?.content
            } else {
                promptResponse.msg?.showToast()
            }
        }, onError = {
            promptGenerating.value = false to ""
        })
    }

    fun regenerate() {
        val content = content.value
        if (content.isNullOrEmpty()) {
            "请输入画面描述".showToast()
            return
        }
        //todo 需要补充埋点参数 EventManager.onAppTrack(AppEvent.JG_CLICK_REDRAW_THE_STORYBOARDS_BUTTON, GrowingIO.KEY_ALL_COUNT_INT to )
        launchWithLoading {
            response(mRepo.shortDramasSegmentImageRegenerate(segment?.shotSegmentId, content)) {
                "重绘分镜成功".showToast()
                dismiss()
                FlowEventBus.postSync(Events.ShortDramasUpdatedEvent(dramasId))
            }
        }
    }

    private fun ShortDramaRoleInfo.toItemStoryboardRoleVM(): ItemStoryboardRoleVM = ItemStoryboardRoleVM(this).also {
        it.onItemClick.value = { v ->
            if (v?.id == R.id.btnDeleteRole) {
                topActivity?.showCommonDialog("删除后生成画面将不会出现该角色，请谨慎操作", "确认要删除角色吗？", "删除") {
                    adapter.remove(it)
                }
            }
        }
    }

    private fun addRole() {
        val existRoleIds = adapter.items.mapNotNull { it.data.get()?.roleId }.let { ArrayList(it) }
        ChooseRoleDialog.show(null, existRoleIds)
    }

    private fun updateAddItem() {
        val existRoleSize = adapter.items.count { !it.data.get()?.roleId.isNullOrEmpty() }
        if (existRoleSize < (parentVM?.shortDrama?.get()?.roleList?.size ?: 0)) {
            if (!adapter.items.contains(addItem)) {
                adapter.add(addItem)
            }
        } else {
            adapter.remove(addItem)
        }
    }
}