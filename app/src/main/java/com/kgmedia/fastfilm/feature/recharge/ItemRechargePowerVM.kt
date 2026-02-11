package com.kgmedia.fastfilm.feature.recharge

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel

/**
 * 充值算力选项
 */
class ItemRechargePowerVM() : AppItemViewModel<Any>(null) {
    val isChecked by lazy { ObservableBoolean(false) }
}