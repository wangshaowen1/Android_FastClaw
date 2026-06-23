package com.shiyuan.fastclaw.core.network.apis.response

import com.shiyuan.fastclaw.core.network.apis.PagedList
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

/**
 * 面试报告列表项响应数据
 *
 * @param reportId 列表统一主展示 ID
 * @param sourceBizId 原业务 ID
 * @param reportType 报告类型
 * @param reportName 报告名称
 * @param reportStatus 统一报告状态
 * @param score 列表分数
 * @param title 分数标签或业务标签
 * @param companyName 公司名称
 * @param jobTitleStd 岗位名称
 * @param createTime 创建或最近活跃时间
 * @param currentStage 当前业务阶段
 * @param progress 进度百分比
 * @param displayTitle 卡片主标题
 * @param displaySubtitle 卡片副标题
 * @param estimateMessage 预计耗时或状态文案
 * @param failReason 失败文案
 * @param canViewReport 是否可查看详情
 * @param nextAction 下一步动作
 * @param retrySupported 是否支持重新生成
 * @param retryConsumePoints 重新生成是否需要额外消耗
 * @param detailPath 前端详情页路径
 * @param detailQuery 前端详情页查询参数
 * @param detailUrl 完整跳转地址
 */
data class InterviewReportListItem(
    val reportId: String?,
    val sourceBizId: String?,
    val reportType: String?,
    val reportName: String?,
    val reportStatus: String?,
    val score: Int?,
    val title: String?,
    val companyName: String?,
    val jobTitleStd: String?,
    val createTime: String?,
    val currentStage: String?,
    val progress: Int = 0,
    val displayTitle: String?,
    val displaySubtitle: String?,
    val estimateMessage: String?,
    val failReason: String?,
    val canViewReport: Boolean?,
    val nextAction: String?,
    val retrySupported: Boolean?,
    val retryConsumePoints: Boolean?,
    val detailPath: String?,
    val detailQuery: String?,
    val detailUrl: String?,
) {
    fun toReportInfo(): ReportInfo {
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
}

fun PagedList<InterviewReportListItem>.toInterviewReportInfoList(): List<ReportInfo> {
    return getItems()?.map { it.toReportInfo() } ?: emptyList()
}
