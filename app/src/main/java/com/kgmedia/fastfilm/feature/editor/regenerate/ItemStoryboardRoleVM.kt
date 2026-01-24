package com.kgmedia.fastfilm.feature.editor.regenerate

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaRoleInfo

/**
 * 重绘分镜-角色
 */
class ItemStoryboardRoleVM(data: ShortDramaRoleInfo?, val addRole: Boolean = false) : AppItemViewModel<ShortDramaRoleInfo>(data) {
    val selected by lazy { ObservableBoolean(false) }
    fun select() {
        selected.set(!selected.get())
    }
}