package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 宝典页业务模块。
 */
data class JobAgentModuleData(
    /** 业务模块对应的统一报告类型，APP 使用该值识别并进入对应业务。 */
    val reportType: String? = null,
    /** 业务模块标题。 */
    val title: String? = null,
    /** 业务模块描述。 */
    val description: String? = null,
    /** 业务模块权益或价格说明。 */
    val benefit: String? = null,
    /** 是否展示免费标识。 */
    val free: Boolean? = null,
) {
    companion object {
        /** 测简历。 */
        const val TYPE_RESUME_REJECTION_ANALYSIS = "RESUME_REJECTION_ANALYSIS"
        /** 测薪资。 */
        const val TYPE_SALARY_VALUATION = "SALARY_VALUATION"
        /** 公司背调。 */
        const val TYPE_COMPANY_RESEARCH = "COMPANY_RESEARCH"
        /** 模拟面试。 */
        const val TYPE_MOCK_INTERVIEW = "MOCK_INTERVIEW"
        /** 面试通关包。 */
        const val TYPE_INTERVIEW_PASS_PACKAGE_REPORT = "INTERVIEW_PASS_PACKAGE_REPORT"
    }
}
