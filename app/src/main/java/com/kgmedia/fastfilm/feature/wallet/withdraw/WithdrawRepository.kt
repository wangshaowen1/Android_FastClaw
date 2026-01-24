package com.kgmedia.fastfilm.feature.wallet.withdraw

import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.request.BindWithdrawAccountBody
import com.kgmedia.fastfilm.core.network.apis.request.BindZfbBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.network.apis.request.WithdrawBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 收益提现
 */
class WithdrawRepository : AppRepository() {
    /**
     * 绑定账号
     */
    suspend fun bindWithdrawAccount(body: BindWithdrawAccountBody) = withContext(Dispatchers.IO) { api.bindWithdrawAccount(body) }

    /**
     * 获取支付宝信息(并绑定)
     */
    suspend fun bindZfb(body: BindZfbBody) = withContext(Dispatchers.IO) { api.bindZfb(body) }

    /**
     * 余额提现
     * @param balance 提现余额
     * @param accountType 提现类型:1=微信 2=支付宝
     */
    suspend fun withdraw(balance: Float, accountType: Int) = withContext(Dispatchers.IO) { api.withdraw(WithdrawBody(balance, accountType)) }

    /**
     * 收益提现详情
     */
    suspend fun withdrawInfo() = withContext(Dispatchers.IO) { api.withdrawInfo(UserTokenBody()) }

    /**
     * 提现确认
     */
    suspend fun withdrawConfirm(balance: Float, accountType: Int) = withContext(Dispatchers.IO) { api.withdrawConfirm(WithdrawBody(balance, accountType)) }
}