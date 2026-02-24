package com.kgmedia.fastfilm.core.network.apis.entity

/**
 * {
 *     "pointConfigId": "算力信息配置ID",
 *     "pointName": "算力名称",
 *     "pointPrice": "实际支付价格（单位：元）",
 *     "pointNum": "算力数量"
 * }
 */
data class PointConfigInfo(
    val pointConfigId: String?,
    val pointName: String?,
    val pointPrice: String?,
    val pointNum: String?
)