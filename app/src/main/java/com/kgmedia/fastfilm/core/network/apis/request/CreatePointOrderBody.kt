package com.kgmedia.fastfilm.core.network.apis.request

data class CreatePointOrderBody(
    /**
     * 商品编号目前是算力配置表的编号
     */
    val goodsCode: String?,
    /**
     * 商品类型1-算力
     */
    val goodsType: Int?,
    /**
     * 支付方式：1 - 微信支付，2 - 支付宝支付，3 - 苹果内购支付
     */
    val payWayType: Int?,
    /**
     * 手机类型：1 - Android，2 - iOS
     */
    val mobileType: Int? = 1,
) : UserTokenBody()