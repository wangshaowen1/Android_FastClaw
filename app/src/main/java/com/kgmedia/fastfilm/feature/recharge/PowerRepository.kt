package com.kgmedia.fastfilm.feature.recharge

import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PowerRepository : AppRepository() {

    /**
     * 算力信息配置列表
     */
    suspend fun pointConfigList() = withContext(Dispatchers.IO) { api.pointConfigList(UserTokenBody()) }

    /**
     * 交易支付方式配置列表
     */
    suspend fun paymentConfigList() = withContext(Dispatchers.IO) { api.paymentConfigList(UserTokenBody()) }

}