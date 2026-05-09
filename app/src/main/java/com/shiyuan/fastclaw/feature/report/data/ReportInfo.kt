package com.shiyuan.fastclaw.feature.report.data

/**
 * 报告列表数据模型
 */
data class ReportInfo(
    val id: String,
    val title: String,
    val score: Int,
    val status: ReportStatus,
    val companyName: String?,
    val position: String?,
    val date: String,
    val tag: String?,
    val tagColor: String?,
    val progressText: String?,
    val progressPercent: Int?
)

enum class ReportStatus {
    /** 生成中 */
    GENERATING,
    /** 已完成 */
    COMPLETED
}
