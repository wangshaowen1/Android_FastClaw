package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 宝典页业务模块。
 */
data class JobAgentModuleData(
    val reportType: String? = null,
    val title: String? = null,
    val description: String? = null,
    val benefit: String? = null,
    val free: Boolean? = null,
) {
    companion object {
        const val TYPE_RESUME_REJECTION_ANALYSIS = "RESUME_REJECTION_ANALYSIS"
        const val TYPE_SALARY_VALUATION = "SALARY_VALUATION"
        const val TYPE_COMPANY_RESEARCH = "COMPANY_RESEARCH"
        const val TYPE_MOCK_INTERVIEW = "MOCK_INTERVIEW"
        const val TYPE_INTERVIEW_PASS_PACKAGE_REPORT = "INTERVIEW_PASS_PACKAGE_REPORT"
    }
}
