package com.kgmedia.fastfilm.feature.works.assetmanager.roleimage

import android.view.View
import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.RoleImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.feature.works.assetmanager.child.AssetsEvent
import com.nhsd.basic.item.BaseItemEventsViewModel

class ItemRoleImageVm(
    data: RoleImageInfo?,
    val model: Boolean = false,
    val itemClickVm: BaseItemEventsViewModel<RoleImageEvent, RoleImageInfo>
) : AppItemViewModel<RoleImageInfo>(data) {
    val selectModel = ObservableBoolean(false)
    val checked = ObservableBoolean(false)

    init {
        selectModel.set(model)
    }

    fun onItemClick(v: View?) {
        if (model) {
            itemClickVm.postEvent(RoleImageEvent.Select(this))
        } else {
            itemClickVm.postEvent(RoleImageEvent.Click(this))
        }
    }
}