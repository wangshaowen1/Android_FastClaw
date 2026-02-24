package com.kgmedia.fastfilm.feature.recharge

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.PointConfigInfo

/**
 * 充值算力选项
 */
class ItemRechargePowerVM(info: PointConfigInfo) : AppItemViewModel<PointConfigInfo>(info) {
    val isChecked by lazy { ObservableBoolean(false) }
}