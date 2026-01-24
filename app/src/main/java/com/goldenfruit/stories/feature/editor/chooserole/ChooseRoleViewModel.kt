package com.kgmedia.fastfilm.feature.editor.chooserole

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaRoleInfo
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorRepository
import com.kgmedia.fastfilm.feature.editor.regenerate.ItemStoryboardRoleVM

/**
 * 重绘分镜-选择角色
 */
class ChooseRoleViewModel : AppRepositoryViewModel<ShortDramaEditorRepository>() {
    val adapter by lazy { QuickAdapter<ItemStoryboardRoleVM>(R.layout.item_storyboard_role, emptyList()) }
    val buttonEnabled by lazy { ObservableBoolean() }

    private fun ShortDramaRoleInfo.toItemStoryboardRoleVM(): ItemStoryboardRoleVM = ItemStoryboardRoleVM(this).also {
        it.onItemClick.value = { v ->
            it.selected.set(!it.selected.get())
            buttonEnabled.set(adapter.items.any { item -> item.selected.get() })
        }
    }

    fun setRoleList(roles: List<ShortDramaRoleInfo>) {
        adapter.submitList(roles.map { it.toItemStoryboardRoleVM() })
    }

    fun useSelectedRoles() {
    }
}