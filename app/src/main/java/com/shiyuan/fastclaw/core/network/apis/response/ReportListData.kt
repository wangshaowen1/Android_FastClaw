package com.shiyuan.fastclaw.core.network.apis.response

import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

data class ReportListData(
    val list: List<ReportListItem>?
)

data class ReportListItem(
    val id: String?,
    val title: String?,
    val score: Int?,
    val status: Int?,
    val companyName: String?,
    val position: String?,
    val date: String?,
    val tag: String?,
    val tagColor: String?,
    val progressText: String?,
    val progressPercent: Int?
) {
    fun toReportInfo() = ReportInfo(
        id = id.orEmpty(),
        title = title.orEmpty(),
        score = score ?: 0,
        status = when (status) {
            1 -> ReportStatus.GENERATING
            else -> ReportStatus.COMPLETED
        },
        companyName = companyName,
        position = position,
        date = date.orEmpty(),
        tag = tag,
        tagColor = tagColor,
        progressText = progressText,
        progressPercent = progressPercent
    )
}

fun ReportListData.toReportInfoList(): List<ReportInfo> {
    return list?.map { it.toReportInfo() } ?: emptyList()
}
