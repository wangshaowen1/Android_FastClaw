package com.shiyuan.fastclaw.core.network.apis.request

/** 更新机会状态请求体。 */
data class OpportunityStatusUpdateBody(
    val jobId: String,
    val actionCode: String,
)
