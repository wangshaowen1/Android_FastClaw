package com.kgmedia.fastfilm.feature.recharge

import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kuaige.payment.PaymentClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PowerRepository : AppRepository() {
//    /**
//     * 获取K币价格配置信息
//     */
//    suspend fun getPowerConfig() = withContext(Dispatchers.IO) {
//        api.rechargeKCoinConfig(UserTokenBody())
//    }

    suspend fun getUserInfo() = withContext(Dispatchers.IO) {
        api.getUserHomeInfo(UserTokenBody())
    }
//
//    /**
//     * 购买K币
//     * @param paymentMethod 支付方式
//     * @param item 充值选项
//     * @param rechargeEntrySource 充值入口来源
//     * @param rechargeEntrySourceId 充值入口来源Id rechargeEntrySource=6时传新用户写歌弹窗配置id
//     */
//    suspend fun recharge(paymentMethod: PaymentClient.PaymentMethod?, item: RechargeKCoinItem, rechargeEntrySource: Int?, rechargeEntrySourceId: String?) =
//        withContext(Dispatchers.IO) {
//            if (item is RechargePlanItem) {
//                val body = FirstRechargeBody(
//                    item.rechargePlanId,
//                    item.rechargePlanItemId,
//                    rechargeEntrySource,
//                    if (rechargeEntrySource == RechargeFrom.NEW_USER_GUIDE_DIALOG) rechargeEntrySourceId else null
//                )
//                if (paymentMethod == PaymentClient.PaymentMethod.Alipay) api.firstRechargeByAlipay(body) else api.firstRechargeByWx(body)
//            } else {
//                val body = RechargeBody(
//                    (item as? RechargeKCoinConfigData.DiamondConfig)?.diamondConfigId,
//                    rechargeEntrySource,
//                    rechargeEntrySourceId
//                )
//                if (paymentMethod == PaymentClient.PaymentMethod.Alipay) api.rechargeByAlipay(body) else api.rechargeByWx(body)
//            }
//        }

}