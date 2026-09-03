package com.shiyuan.fastclaw.feature.report.data

import com.shiyuan.fastclaw.core.network.apis.PagedList
import com.shiyuan.fastclaw.core.network.apis.response.ReportListItem

/** 将报告列表接口 DTO 转换为报告 feature 使用的展示模型。 */
fun ReportListItem.toReportInfo(): ReportInfo {
    return ReportInfo(
        id = reportId.orEmpty(),
        title = title ?: reportName.orEmpty(),
        status = when (reportStatus) {
            "RUNNING", "GENERATING", "RETRY_WAITING" -> ReportStatus.IN_PROGRESS
            "FAILED" -> ReportStatus.FAILED
            else -> ReportStatus.OTHER
        },
        companyName = companyName,
        position = jobTitleStd,
        createTime = createTime.orEmpty(),
        tag = tag,
        tagColor = tagColor,
        progressText = null,
        progress = progress,
        reportType = reportType,
        reportName = reportName,
        canViewReport = canViewReport ?: false,
        detailUrl = detailUrl,
        jobTitleStd = jobTitleStd,
        detailQuery = detailQuery,
        estimateMessage = estimateMessage,
        failReason = failReason,
        totalScore = totalScore ?: 0,
    )
}

fun PagedList<ReportListItem>.toReportInfoList(): List<ReportInfo> {
    return getItems()?.map { it.toReportInfo() }.orEmpty()
}
