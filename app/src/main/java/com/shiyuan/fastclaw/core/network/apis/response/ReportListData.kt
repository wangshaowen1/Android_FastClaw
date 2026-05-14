package com.shiyuan.fastclaw.core.network.apis.response

import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

data class ReportListData(
    val total: String?,
    val pageNum: String?,
    val pageSize: String?,
    val pages: String?,
    val hasNext: Boolean?,
    val list: List<ReportListItem>?
)

data class ReportListItem(
    val reportId: String?,
    val reportName: String?,
    val companyName: String?,
    val jobTitleStd: String?,
    val reportType: String?,
    val reportStatus: String?,
    val currentStage: String?,
    val progress: Int?,
    val totalScore: Int?,
    val title: String?,
    val createTime: String?,
    val tag: String? = null,
    val tagColor: String? = null,
    val canViewReport: Boolean? = null,
    val detailUrl: String? = null,
    val detailQuery: String? = null,
    val estimateMessage: String? = null
) {
    fun toReportInfo() = ReportInfo(
        id = reportId.orEmpty(),
        title = title ?: reportName.orEmpty(),
        status = when (reportStatus) {
            "RUNNING", "GENERATING"-> ReportStatus.IN_PROGRESS
            else -> ReportStatus.OTHER
        },
        companyName = companyName,
        position = jobTitleStd,
        createTime = createTime.orEmpty(),
        tag = tag,
        tagColor = tagColor,
        progressText = null,
        progressPercent = progress,
        reportType = reportType,
        reportName = reportName,
        canViewReport = canViewReport ?: false,
        detailUrl = detailUrl,
        jobTitleStd = jobTitleStd,
        detailQuery = detailQuery,
        estimateMessage = estimateMessage,
        totalScore = totalScore ?: 0,
    )
}

fun ReportListData.toReportInfoList(): List<ReportInfo> {
    return list?.map { it.toReportInfo() } ?: emptyList()
}
