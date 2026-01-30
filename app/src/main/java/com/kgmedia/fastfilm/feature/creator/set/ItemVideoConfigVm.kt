package com.kgmedia.fastfilm.feature.creator.set

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigInfo

class ItemVideoConfigVm(ratio: VideoConfigInfo) : AppItemViewModel<VideoConfigInfo>(ratio) {
    val checked = ObservableBoolean()

    val input = ObservableField<String>()
}