package com.shiyuan.fastclaw.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 虾条充值套餐列表
 */
data class PointConfigListData(
    val pointConfigList: List<PointConfigInfo>?
)

/**
 * 虾条充值套餐
 */
@Parcelize
data class PointConfigInfo(
    /**
     * 商品编号，前端下单时传入 goodsCode。
     */
    val goodsCode: Long?,
    /**
     * 商品类型：1-虾条充值。
     */
    val goodsType: Int?,
    /**
     * 商品名称，和下单链路 GoodsVO 保持一致。
     */
    val goodsName: String?,
    /**
     * 商品支付价格，单位：元，仅用于前端展示，不作为下单金额校验依据。
     */
    val goodsPrice: String?,
    /**
     * 到账虾条数量。
     */
    val pointNum: Long?,
    /**
     * Apple IAP 商品 ID，iOS 内购使用。
     */
    val appleProductId: String?,
    /**
     * 商品描述，和下单链路 GoodsVO 保持一致。
     */
    val goodsDesc: String?,
    /**
     * 虾条套餐名称，用于页面展示。
     */
    val pointName: String?,
    /**
     * 支付价格展示文案，例如 ¥30。
     */
    val priceText: String?,
    /**
     * 是否默认选中（0-非默认、1-默认）
     */
    val isDefault: Int?
) : Parcelable
