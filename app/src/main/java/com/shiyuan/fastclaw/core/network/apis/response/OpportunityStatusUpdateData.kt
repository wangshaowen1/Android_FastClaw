package com.shiyuan.fastclaw.core.network.apis.response

/** 更新机会状态结果。 */
data class OpportunityStatusUpdateData(
    val status: String = "",
    val statusTime: String? = null,
)
