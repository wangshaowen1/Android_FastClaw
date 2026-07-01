package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 候选人支付成功订单
 */
data class PayOrderInfo(
    /**
     * 商户订单号
     */
    val orderNo: String? = null,
    /**
     * 商品名称，优先取下单时的商品快照
     */
    val goodsName: String? = null,
    /**
     * 实付金额展示文案，例如 68.00元
     */
    val payAmountText: String? = null,
    /**
     * 订单创建时间，格式 yyyy-MM-dd HH:mm:ss
     */
    val createTime: String? = null
)
