package com.kgmedia.fastfilm.feature.invitation.income

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.common.utils.Utils
import com.kgmedia.fastfilm.core.network.apis.response.InviteIncomePageInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请收益列表
 * @Data: 2025/10/13/周一  14:54
 * @Version: 1.0
 * *****************************************
 */
class ItemInvitationIncomeVm(data: InviteIncomePageInfo?) :
    AppItemViewModel<InviteIncomePageInfo>(data) {

    fun remarks(): String {
        return DateUtil.format(data.get()?.inviteTime ?: 0L, DateUtil.FORMAT_DATETIME_BIAS) + " " + data.get()?.inviteRemark
    }

    fun amount(): String {
        //把金额除以1000在格式化成最多保留俩位小数点，如果后面是零就省略
        return (if (data.get()?.tradeType == 2) {
            "-"
        } else {
            "+"
        }) + data.get()?.commissionAmount?.formatMoney() + "元"
    }
}