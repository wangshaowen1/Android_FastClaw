package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 创建支付订单请求体
 *
 * @param goodsCode 商品编号，目前是虾条配置表的编号
 * @param goodsType 商品类型：1-虾条
 */
data class TradePayCreateBody(
    val goodsCode: Long,
    val goodsType: Int = GOODS_TYPE_POINT
) : UserTokenBody() {
    companion object {
        const val GOODS_TYPE_POINT = 1
    }
}
