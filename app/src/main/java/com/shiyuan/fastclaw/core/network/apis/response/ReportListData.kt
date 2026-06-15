package com.shiyuan.fastclaw.core.network.apis.response

import com.shiyuan.fastclaw.core.network.apis.PagedList
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

/**
 * 报告列表项响应数据
 *
 * @param reportId       报告ID
 * @param reportName    报告名称
 * @param companyName   公司名称
 * @param jobTitleStd   标准职位名称
 * @param reportType    报告类型
 * @param reportStatus  报告状态：RUNNING/GENERATING/RETRY_WAITING-进行中 / FAILED-失败 / 其他-其他状态
 * @param currentStage  当前阶段
 * @param progress      进度百分比（0-100）
 * @param totalScore    总分
 * @param title         标题
 * @param createTime    创建时间
 * @param tag           标签
 * @param tagColor      标签颜色
 * @param canViewReport 是否可以查看报告
 * @param detailUrl     详情页URL
 * @param detailQuery   详情页查询参数
 * @param estimateMessage 预估信息
 * @param failReason    失败原因
 */
data class ReportListItem(
    val reportId: String?,
    val reportName: String?,
    val companyName: String?,
    val jobTitleStd: String?,
    val reportType: String?,
    val reportStatus: String?,
    val currentStage: String?,
    val progress: Int = 0,
    val totalScore: Int?,
    val title: String?,
    val createTime: String?,
    val tag: String? = null,
    val tagColor: String? = null,
    val canViewReport: Boolean? = null,
    val detailUrl: String? = null,
    val detailQuery: String? = null,
    val estimateMessage: String? = null,
    val failReason: String? = null
) {
    fun toReportInfo() = ReportInfo(
        id = reportId.orEmpty(),
        title = title ?: reportName.orEmpty(),
        status = when (reportStatus) {
            "RUNNING", "GENERATING","RETRY_WAITING"-> ReportStatus.IN_PROGRESS
            "FAILED"-> ReportStatus.FAILED
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
    return getItems()?.map { it.toReportInfo() } ?: emptyList()
}
