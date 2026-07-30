package com.shiyuan.fastclaw.core.network.apis.response

import com.shiyuan.fastclaw.core.network.apis.entity.PaymentData

data class H5InterviewPassPackageRep(
    val packageId: String? = null,
    val orderNo: String? = null,
    val goodsName: String? = null,
    val goodsCode: String? = null,
    val totalAmount: String? = null,
    val payWay: String? = null,
    val payOrder: PaymentData? = null
){
    companion object {
        const val CLEARANCE_PACKAGE = "90001001"
        const val SALARY_IS_UNDERESTIMATED = "90001003"
        const val ONE_REPLIED_RESUME = "90001002"
    }

}
