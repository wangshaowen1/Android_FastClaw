package com.kgmedia.fastfilm.feature.payment.points

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.core.network.apis.response.ScoreConfigData

/**
 * 首次充值弹窗
 */
class ItemFirstTimeRechargeVM(data: ScoreConfigData.PointsData?) : AppItemViewModel<ScoreConfigData.PointsData>(data) {
    val selected by lazy { ObservableBoolean() }
    val pointAmount by lazy { data?.pointAmount?.formatMoney()?.let { "¥$it" } }
}