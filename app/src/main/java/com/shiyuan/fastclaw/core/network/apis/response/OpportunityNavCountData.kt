package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 机会导航未处理数量。
 */
data class OpportunityNavCountData(
    /** 当前有效且尚未处理的新机会数量。 */
    val unprocessedCount: Int = 0,
)
