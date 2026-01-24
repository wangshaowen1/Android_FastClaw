package com.kgmedia.fastfilm.feature.wallet.withdraw

import com.kgmedia.fastfilm.base.viewmodel.AppViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawAccountInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawConfirmInfo

/**
 * 提现确认
 */
class WithdrawConfirmViewModel : AppViewModel() {
    val amount by lazy { mBundle.getString("amount") }
    val info by lazy { mBundle.getSerializable("info") as? WithdrawConfirmInfo }
    val account by lazy { mBundle.getSerializable("account") as? WithdrawAccountInfo }
}