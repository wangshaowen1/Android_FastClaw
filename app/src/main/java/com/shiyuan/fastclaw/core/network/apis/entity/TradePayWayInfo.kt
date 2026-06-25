package com.shiyuan.fastclaw.core.network.apis.entity

/**
 * 安卓充值页支付方式列表
 */
data class TradePayWayListData(
    val itemList: List<TradePayWayInfo>?
)

/**
 * 安卓充值页支付方式
 *
 * @param payWay 支付方式：1-微信支付，2-支付宝支付，3-苹果内购支付，4-苹果沙盒内购支付，5-抖音支付，6-宝付支付。
 * @param payWayName 支付方式名称，用于前端展示。
 * @param isDefault 是否默认选中：0-非默认，1-默认。
 * @param sortNo 支付方式展示排序值，数值越大越靠前。
 */
data class TradePayWayInfo(
    val payWay: Int?,
    val payWayName: String?,
    val isDefault: Int?,
    val sortNo: Int?
) {
    companion object {
        const val PAY_WAY_WECHAT = 1
        const val PAY_WAY_ALIPAY = 2
        const val PAY_WAY_BAOFU = 6
    }
}
