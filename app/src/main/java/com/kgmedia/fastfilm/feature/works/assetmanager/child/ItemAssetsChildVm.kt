package com.kgmedia.fastfilm.feature.works.assetmanager.child

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset

class ItemAssetsChildVm(
    data: SimpleAsset?
) : AppItemViewModel<SimpleAsset>(data) {
    val checked = ObservableBoolean(false)
}