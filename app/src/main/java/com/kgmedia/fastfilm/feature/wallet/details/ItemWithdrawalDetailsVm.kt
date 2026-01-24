package com.kgmedia.fastfilm.feature.wallet.details

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.response.WithdrawalRecordInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:提现明细
 * @Data: 2025/10/13/周一  14:54
 * @Version: 1.0
 * *****************************************
 */
class ItemWithdrawalDetailsVm(data: WithdrawalRecordInfo?) :
    AppItemViewModel<WithdrawalRecordInfo>(data) {

    fun remarks(): String {
        return DateUtil.format(data.get()?.createTime ?: 0L, DateUtil.FORMAT_DATETIME_BIAS)
    }

    fun amount(): String {
        //把金额除以1000在格式化成最多保留俩位小数点，如果后面是零就省略
        return data.get()?.withdrawalAmount?.formatMoney() + "元"
    }
}