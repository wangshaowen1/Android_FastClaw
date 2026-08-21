package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 最新推荐批次中的新机会。
 */
data class OpportunityNewItemData(
    /** 岗位 ID。 */
    val jobId: String = "",
    /** 岗位名称。 */
    val jobTitle: String = "",
    /** 公司显示名称。 */
    val companyName: String = "",
    /** 后端组装完成的薪资文案。 */
    val salaryText: String = "",
    /** 工作城市展示文本。 */
    val workCity: String = "",
    /** 工作经验要求展示文本。 */
    val experienceText: String = "",
    /** 学历要求展示文本。 */
    val degreeText: String = "",
    /** 匹配度，范围 0～100。 */
    val matchPercent: Int = 0,
    /** 推荐理由，为空时不展示推荐理由区域。 */
    val recommendationReason: String = "",
    /** 是否从未查看过岗位详情。 */
    val unread: Boolean = false,
)
