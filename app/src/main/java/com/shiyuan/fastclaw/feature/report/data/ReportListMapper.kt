package com.shiyuan.fastclaw.feature.report.data

import com.shiyuan.fastclaw.core.network.apis.PagedList
import com.shiyuan.fastclaw.core.network.apis.response.InterviewReportListItem
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

fun InterviewReportListItem.toReportInfo(): ReportInfo {
    val cardTitle = displayTitle?.takeIf { it.isNotBlank() }
    val useCardTitle = cardTitle != null
    return ReportInfo(
        id = reportId ?: sourceBizId.orEmpty(),
        title = cardTitle ?: title ?: reportName.orEmpty(),
        status = when (reportStatus) {
            "RUNNING" -> ReportStatus.IN_PROGRESS
            "FAILED" -> ReportStatus.FAILED
            else -> ReportStatus.OTHER
        },
        companyName = if (useCardTitle) null else companyName,
        position = if (useCardTitle) null else jobTitleStd,
        tag = null,
        tagColor = null,
        progress = progress,
        progressText = null,
        jobTitleStd = if (useCardTitle) null else jobTitleStd,
        reportType = reportType,
        createTime = createTime.orEmpty(),
        reportName = cardTitle ?: reportName,
        canViewReport = canViewReport ?: false,
        detailUrl = detailUrl,
        detailQuery = detailQuery,
        detailPath = detailPath,
        estimateMessage = estimateMessage ?: displaySubtitle,
        failReason = failReason,
        totalScore = score ?: 0,
    )
}

fun PagedList<InterviewReportListItem>.toInterviewReportInfoList(): List<ReportInfo> {
    return getItems()?.map { it.toReportInfo() }.orEmpty()
}
