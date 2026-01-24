package com.kgmedia.fastfilm.feature.payment.points.details

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.response.RechargePageInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:积分充值
 * @Data: 2025/4/17  14:40
 * @Version: 1.0
 * *****************************************
 */
class ItemConsumptionDetailsVm(
    data: RechargePageInfo?
) : AppItemViewModel<RechargePageInfo>(data) {

    fun time(): String {
        return DateUtil.format(data.get()?.detailTime ?: 0L, DateUtil.FORMAT_DATE5)
    }
}