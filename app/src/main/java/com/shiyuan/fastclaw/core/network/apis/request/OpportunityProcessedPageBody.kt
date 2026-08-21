package com.shiyuan.fastclaw.core.network.apis.request

/** 按状态分页查询已处理机会。 */
data class OpportunityProcessedPageBody(
    /** 只允许 COMMUNICATING 或 UNSUITABLE。 */
    val status: String,
    val pageNum: Int,
    val pageSize: Int,
)
