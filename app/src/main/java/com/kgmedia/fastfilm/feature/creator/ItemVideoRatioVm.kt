package com.kgmedia.fastfilm.feature.creator

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.request.CreationGeneratePreBody

class ItemVideoRatioVm(ratio: Int) : AppItemViewModel<Int>(ratio) {

    val checked = ObservableBoolean(ratio == CreationGeneratePreBody.RATIO_9_16)

    fun getText(): String {
        return when (data.get()) {
            CreationGeneratePreBody.RATIO_9_16 -> "9:16"
            CreationGeneratePreBody.RATIO_16_9 -> "16:9"
            CreationGeneratePreBody.RATIO_3_4 -> "3:4"
            CreationGeneratePreBody.RATIO_4_3 -> "4:3"
            else -> ""
        }
    }

    fun getWidth(): Int {
        return when (data.get()) {
            CreationGeneratePreBody.RATIO_9_16 -> 11
            CreationGeneratePreBody.RATIO_16_9 -> 19
            CreationGeneratePreBody.RATIO_3_4 -> 15
            CreationGeneratePreBody.RATIO_4_3 -> 18
            else -> 0 // 或者返回默认值
        }
    }

    fun getHeight(): Int {
        return when (data.get()) {
            CreationGeneratePreBody.RATIO_9_16 -> 19
            CreationGeneratePreBody.RATIO_16_9 -> 11
            CreationGeneratePreBody.RATIO_3_4 -> 18
            CreationGeneratePreBody.RATIO_4_3 -> 15
            else -> 0 // 或者返回默认值
        }
    }
}