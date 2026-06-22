package com.shiyuan.fastclaw.core.network.apis.entity

data class PaymentData(
    /**支付-签名字符串*/
    val orderStr: String?,
    /**支付信息，宝付 APP 预下单时为小程序跳转 URI*/
    val payInfo: String? = null,
    /**订单编号*/
    val orderNo: String?,
    /**(目前这里只有微信支付有) 支付方式 0-微信支付 1-宝付*/
    val payWay: Int,
    /**(目前这里只有微信支付有) 宝付 跳转小程序原始ID*/
    val originalId: String?,
    /**宝付小程序 JSAPI 下单时间*/
    val orderTime: String? = null,
    /**宝付小程序 JSAPI 微信支付数据*/
    val wxPayData: String? = null
) {
    val isBaoFu get() = payWay == 1
}
