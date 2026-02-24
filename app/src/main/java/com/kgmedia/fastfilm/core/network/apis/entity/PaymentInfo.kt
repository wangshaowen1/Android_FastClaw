package com.kgmedia.fastfilm.core.network.apis.entity

/**
 * {
 *     "wayConfigCode": "支付方式配置编号（雪花算法ID）",
 *     "payWay": "支付方式（1-微信、2-支付宝、3-苹果内购）",
 *     "waySortNo": "展示排序值（从大到小，越大越靠前）",
 *     "payMerchantConfigId": "支付商配置表ID（苹果内购不需要设置）"
 * }
 */
data class PaymentInfo(
    /**
     * 支付方式配置编号（雪花算法ID）
     */
    val wayConfigCode: String?,
    /**
     * 支付方式（1-微信、2-支付宝、3-苹果内购）
     */
    val payWay: Int?,
    /**
     * 展示排序值（从大到小，越大越靠前）
     */
    val waySortNo: Int?,
    /**
     * 支付商配置表ID（苹果内购不需要设置）
     */
    val payMerchantConfigId: Int?
)