package com.shiyuan.fastclaw.core.network.apis.response

/** 机会详情。 */
data class OpportunityDetailData(
    val jobId: String = "",
    val jobTitle: String = "",
    val matchPercent: Int = 0,
    val status: String = "",
    val jobClosed: Boolean = false,
    val enterpriseId: String = "",
    val companyName: String = "",
    val salaryText: String = "",
    val workCity: String = "",
    val experienceText: String = "",
    val degreeText: String = "",
    val recommendationReason: String = "",
    val jobDescription: String = "",
    val companyIndustry: String = "",
    val companyScale: String = "",
    val financingStage: String = "",
    val workAddress: String = "",
    val actions: List<OpportunityDetailActionData> = emptyList(),
)

/** 机会详情底部操作，按接口返回顺序展示。 */
data class OpportunityDetailActionData(
    val buttonText: String = "",
    val actionCode: String? = null,
)
