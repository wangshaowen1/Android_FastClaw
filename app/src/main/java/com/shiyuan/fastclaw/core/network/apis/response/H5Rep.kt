package com.shiyuan.fastclaw.core.network.apis.response

import com.shiyuan.fastclaw.core.network.apis.entity.PaymentData

data class H5InterviewPassPackageRep(
    val packageId: String? = null,
    val orderNo: String? = null,
    val goodsName: String? = null,
    val totalAmount: String? = null,
    val payWay: String? = null,
    val payOrder: PaymentData? = null
)
