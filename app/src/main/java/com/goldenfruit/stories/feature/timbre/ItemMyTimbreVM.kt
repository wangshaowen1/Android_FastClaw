package com.kgmedia.fastfilm.feature.timbre

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.TimbreInfo

/**
 * 音色列表
 */
class ItemMyTimbreVM(info: TimbreInfo) : AppItemViewModel<TimbreInfo>(info) {

    val playing by lazy { ObservableBoolean(false) }
    val selected by lazy { ObservableBoolean(false) }
    val name = ObservableField(info.getGenerateTitle())

    var onPlayClick: (() -> Unit?)? = null

    var onLongClick: (() -> Boolean)? = null

    var onGenerateClick: ((String?) -> Unit)? = null

    fun timbreGenerate() {
        onGenerateClick?.invoke(data.get()?.timbreId)
    }
}
