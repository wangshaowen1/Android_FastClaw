package com.kgmedia.fastfilm.feature.details

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.response.ComputingPowerDetailsInfo


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce: 充值明细
 * @Data: 2025/4/17  14:40
 * @Version: 1.0
 * *****************************************
 */
class ItemComputingPowerDetailsVm(
    data: ComputingPowerDetailsInfo?
) : AppItemViewModel<ComputingPowerDetailsInfo>(data) {

    fun time(): String {
        return DateUtil.format(data.get()?.detailTime ?: 0L, DateUtil.FORMAT_DATE5)
    }

    fun quantity(): String {
        val item = data.get() ?: return ""
        return if (item.tradeType == ComputingPowerDetailsInfo.SCORE_TYPE_GET) {
            "+"
        } else {
            "-"
        } + (item.detailAmount ?: "0")
    }
}