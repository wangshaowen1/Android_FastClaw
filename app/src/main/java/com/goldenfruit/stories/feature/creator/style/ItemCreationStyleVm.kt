package com.kgmedia.fastfilm.feature.creator.style

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.CreationStyleInfo

class ItemCreationStyleVm(info: CreationStyleInfo) : AppItemViewModel<CreationStyleInfo>(info) {

    val checked= ObservableBoolean(false)

}