package com.kgmedia.fastfilm.feature.creator.generate.edit

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigInfo

class ItemReferenceImageVm(info: ReferenceImageInfo) : AppItemViewModel<ReferenceImageInfo>(info) {
    val checked = ObservableBoolean()

}