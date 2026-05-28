package com.shiyuan.fastclaw.feature.report.data

/**
 * 报告列表数据模型
 */
data class ReportInfo(
    val id: String,
    val title: String,
    val status: ReportStatus,
    val companyName: String?,
    val position: String?,
    val tag: String?,
    val tagColor: String?,
    val progress: Int = 0,
    val progressText: String?,
    val jobTitleStd: String?,
    val reportType: String? = null,
    val createTime: String? = null,
    val reportName: String? = null,
    val canViewReport: Boolean = false,
    val detailUrl: String? = null,
    val detailQuery: String? = null,
    val detailPath: String? = null,
    val estimateMessage: String? = null,
    val failReason: String? = null,
    val totalScore: Int = 0,
)

enum class ReportStatus {
    /** 生成cg */
    FAILED,
    IN_PROGRESS,
    OTHER,

}
