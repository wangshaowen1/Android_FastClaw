package com.shiyuan.fastclaw.core.network.apis.response

/** 机会关联公司的公开信息。 */
data class OpportunityCompanyDetailData(
    val enterpriseId: Long = 0,
    val brandName: String = "",
    val logoUrl: String = "",
    val enterpriseName: String = "",
    val industryName: String = "",
    val scaleName: String = "",
    val financingStageName: String = "",
    val introduction: String = "",
    val companyCulture: String = "",
    val welfareTags: List<String> = emptyList(),
    val location: String = "",
)
