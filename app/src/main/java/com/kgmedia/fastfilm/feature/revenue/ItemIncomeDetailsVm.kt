package com.kgmedia.fastfilm.feature.revenue

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.entity.ProfitDetailInfo

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:创造收益
 * @Data: 2025/11/4/周二  13:47
 * @Version: 1.0
 * *****************************************
 */
class ItemIncomeDetailsVm(profitDetail: ProfitDetailInfo?) :
    AppItemViewModel<ProfitDetailInfo>(profitDetail) {
    fun transactionDate(): String {
        return DateUtil.format(data.get()?.createTime ?: 0L, DateUtil.FORMAT_DATETIME_BIAS)
    }

    fun amountOfMoney(): String {
        val dataValue = data.get()
        val isIncome = dataValue?.income() == true
        val prefix = if (isIncome) "+" else "-"
        val amount = dataValue?.tradeAmount?.formatMoney()
        return "$prefix${amount}元"
    }
}