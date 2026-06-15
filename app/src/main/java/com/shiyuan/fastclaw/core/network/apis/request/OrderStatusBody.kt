package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 订单状态查询请求体
 *
 * @param orderNo 订单号
 */
data class OrderStatusBody(
    /** 订单号 */
    val orderNo: String? = null
): UserTokenBody()