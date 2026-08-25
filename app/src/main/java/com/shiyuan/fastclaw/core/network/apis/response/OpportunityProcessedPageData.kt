package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 已处理机会分页响应。
 *
 * 同时返回“沟通中”“不合适”两个状态的聚合数量，以及本次查询的分页数据。
 */
data class OpportunityProcessedPageData(
    /** 当前候选人沟通中机会总数。 */
    val communicatingCount: Int = 0,
    /** 当前候选人不合适机会总数。 */
    val unsuitableCount: Int = 0,
    /** 当前页机会及分页信息。 */
    val opportunities: OpportunityProcessedPage = OpportunityProcessedPage(),
)

/** 已处理机会分页信息。分页数字字段由服务端以字符串返回。 */
data class OpportunityProcessedPage(
    /** 当前页的已处理机会列表。 */
    val items: List<OpportunityProcessedItemData> = emptyList(),
    /** 符合查询条件的机会总数。 */
    val total: String = "0",
    /** 当前分页大小。 */
    val pageSize: String = "0",
    /** 当前页码。 */
    val pageNum: String = "0",
    /** 总页数。 */
    val totalPage: String = "0",
)

/** 已处理机会列表条目。 */
data class OpportunityProcessedItemData(
    /** 岗位 ID，也是列表条目的唯一业务标识。 */
    val jobId: String = "",
    /** 岗位名称。 */
    val jobTitle: String = "",
    /** 公司展示名称。 */
    val companyName: String = "",
    /** 服务端组装完成的薪资文案。 */
    val salaryText: String = "",
    /** 工作城市展示文本。 */
    val workCity: String = "",
    /** 工作经验要求展示文本。 */
    val experienceText: String = "",
    /** 学历要求展示文本。 */
    val degreeText: String = "",
    /** 岗位与候选人的匹配度展示值。 */
    val matchPercent: Int = 0,
    /** 机会处理状态，当前使用 [STATUS_COMMUNICATING] 或 [STATUS_UNSUITABLE]。 */
    val status: String = "",
    /** 当前状态对应的处理时间，格式为 yyyy-MM-dd HH:mm:ss。 */
    val statusTime: String = "",
    val jobClosed: Boolean = false,
) {
    companion object {
        /** 已进入沟通状态。 */
        const val STATUS_COMMUNICATING = "COMMUNICATING"
        /** 已标记为不合适。 */
        const val STATUS_UNSUITABLE = "UNSUITABLE"
    }
}
