package com.kgmedia.fastfilm.feature.works.assetmanager.child

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.nhsd.basic.item.BaseItemEventsViewModel

class ItemAssetsChildVm(
    data: SimpleAsset?,
    val model: Boolean = false,
    val itemClickVm: BaseItemEventsViewModel<AssetsEvent, SimpleAsset>
) : AppItemViewModel<SimpleAsset>(data) {
    val selectModel = ObservableBoolean(false)
    val checked = ObservableBoolean(false)

    init {
        selectModel.set(model)
    }

    fun onItemClick() {
        if (model) {
            itemClickVm.postEvent(AssetsEvent.Select(this))
        } else {
            itemClickVm.postEvent(AssetsEvent.Click(this))
        }
    }
}