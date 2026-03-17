package com.kgmedia.fastfilm.core.network.apis.entity

/**
 * {
 *     "pointConfigId": "算力信息配置ID",
 *     "pointConfigCode": "算力信息配置编码下单时要提交的商品编号",
 *     "pointName": "算力名称",
 *     "pointPrice": "实际支付价格（单位：元）",
 *     "pointNum": "算力数量",
 *     "isDefault": "是否默认选中（0-非默认、1-默认）"
 * }
 */
data class PointConfigInfo(
    val pointConfigId: String?,
    val pointConfigCode: String?,
    val pointName: String?,
    val pointPrice: String?,
    val pointNum: String?,
    /**
     * 是否默认选中（0-非默认、1-默认）
     */
    val isDefault: Int?,
    val pointMarketingTime: Long?,//算力营销倒计时截止时间.空则代表不需要受限制
    val pointMarketingText: String?,//算力营销倒计时文本
) {

    fun isDiscountValid(): Boolean {
        return pointMarketingTime != null && pointMarketingTime > System.currentTimeMillis()
    }

    fun getCountDownText(): String {
        if (pointMarketingTime == null) {
            return ""
        }
        val time = pointMarketingTime - System.currentTimeMillis()
        if (time <= 0) {
            return ""
        }
        val hour = time / 1000 / 60 / 60
        val minute = time / 1000 / 60 % 60
        val second = time / 1000 % 60
        return String.format("$pointMarketingText %02d:%02d:%02d", hour, minute, second)
    }
}