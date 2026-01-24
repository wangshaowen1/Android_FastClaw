package com.kgmedia.fastfilm.feature.creator.generate

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.response.ShotSegmentInfo

class ItemStoryboardVM(data: ShotSegmentInfo?) : AppItemViewModel<ShotSegmentInfo>(data) {
    val selected by lazy { ObservableBoolean(false) }
    val showText = ObservableField<String>(data?.sceneContent)

    fun isInputNotComplete(): Boolean {
        return showText.get().isNullOrEmpty()
    }
}