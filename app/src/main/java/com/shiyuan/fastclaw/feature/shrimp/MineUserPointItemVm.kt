package com.shiyuan.fastclaw.feature.shrimp

import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.core.network.apis.response.UserPointItemData
import com.umeng.analytics.pro.da

/**
 * @createtime 2026年05月13日 20:05
 * @author TooWhiteT
 */

class MineUserPointItemVm(data: UserPointItemData?) : AppItemViewModel<UserPointItemData>(data)  {

    fun isSubtractTransaction(): Boolean {

        val transactionType = data.get()?.transactionType?.trim()
        return transactionType.equals("SUBTRACT", ignoreCase = true) ||
                data.get()?.showTransaction()?.trim()?.startsWith("-") == true
    }
}
