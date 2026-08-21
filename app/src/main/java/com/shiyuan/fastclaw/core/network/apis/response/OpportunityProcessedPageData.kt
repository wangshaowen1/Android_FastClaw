package com.shiyuan.fastclaw.core.network.apis.response

/** 已处理机会分页响应。 */
data class OpportunityProcessedPageData(
    /** 当前候选人沟通中机会总数。 */
    val communicatingCount: Int = 0,
    /** 当前候选人不合适机会总数。 */
    val unsuitableCount: Int = 0,
    /** 当前页机会及分页信息。 */
    val opportunities: OpportunityProcessedPage = OpportunityProcessedPage(),
)

data class OpportunityProcessedPage(
    val items: List<OpportunityProcessedItemData> = emptyList(),
    val total: String = "0",
    val pageSize: String = "0",
    val pageNum: String = "0",
    val totalPage: String = "0",
)

/** 已处理机会。 */
data class OpportunityProcessedItemData(
    val jobId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val salaryText: String = "",
    val workCity: String = "",
    val experienceText: String = "",
    val degreeText: String = "",
    val matchPercent: Int = 0,
    val status: String = "",
    /** 当前状态对应的处理时间，格式为 yyyy-MM-dd HH:mm:ss。 */
    val statusTime: String = "",
) {
    companion object {
        const val STATUS_COMMUNICATING = "COMMUNICATING"
        const val STATUS_UNSUITABLE = "UNSUITABLE"
    }
}
