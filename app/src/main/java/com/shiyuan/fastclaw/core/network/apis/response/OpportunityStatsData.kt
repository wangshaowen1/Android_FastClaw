package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 常态化匹配页面的机会统计数据。
 */
data class OpportunityStatsData(
    /** 浏览岗位展示值。 */
    val viewedJobCount: Int = 0,
    /** 推荐机会展示值。 */
    val recommendedOpportunityCount: Int = 0,
    /** 待定机会展示值。 */
    val pendingOpportunityCount: Int = 0,
    /** 不匹配展示值。 */
    val notMatchedOpportunityCount: Int = 0,
)
