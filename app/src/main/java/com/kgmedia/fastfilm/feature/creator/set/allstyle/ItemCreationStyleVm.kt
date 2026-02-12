package com.kgmedia.fastfilm.feature.creator.set.allstyle

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.VideoStyleConfigInfo

class ItemCreationStyleVm(info: VideoStyleConfigInfo) : AppItemViewModel<VideoStyleConfigInfo>(info) {

    val checked= ObservableBoolean(false)

}