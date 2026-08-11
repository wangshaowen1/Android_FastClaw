package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 首页活动状态。
 */
data class ActivityStatusData(
    /** 活动对应的报告类型；当前测薪资活动固定为 SALARY_VALUATION。 */
    val reportType: String? = null,
    /** 是否展示活动卡片。 */
    val enabled: Boolean? = null,
) {
    companion object {
        /** 测薪资活动报告类型。 */
        const val REPORT_TYPE_SALARY_VALUATION = "SALARY_VALUATION"
    }
}
