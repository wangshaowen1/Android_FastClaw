package com.kgmedia.fastfilm.feature.series.setprice

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney

/**
 * 剧集-设置价格-ItemVM
 */
class ItemSetPriceVM(data: Long, val customize: Boolean = false, val free: Boolean = false) : AppItemViewModel<Long>(data) {
    val selected by lazy { ObservableBoolean() }
    val text by lazy {
        if (free) {
            "全部免费"
        } else if (customize) {
            "自定义"
        } else {
            "${data.formatMoney()}元"
        }
    }
    val input by lazy { MutableLiveData(data.takeIf { it > 0 }?.formatMoney()) }
}