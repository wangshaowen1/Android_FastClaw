package com.shiyuan.fastclaw.feature.report.interview

import androidx.databinding.ObservableField
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

/**
 * 报告列表项 ViewModel
 */
class ItemInterviewReportVM(data: ReportInfo?) : AppItemViewModel<ReportInfo>(data) {

    val estimateMessage = ObservableField(data?.estimateMessage.orEmpty())
    val failReason = ObservableField(buildFailReason(data))
    val progress = ObservableField(data?.progress)
    val reportName = ObservableField(buildReportName(data))
    val completedReportName = ObservableField(buildCompletedReportName(data))
    val createTime = ObservableField(data?.createTime.orEmpty())

    override fun updateData(newItem: ReportInfo?) {
        super.updateData(newItem)
        estimateMessage.set(newItem?.estimateMessage.orEmpty())
        failReason.set(buildFailReason(newItem))
        progress.set(newItem?.progress)
        reportName.set(buildReportName(newItem))
        completedReportName.set(buildCompletedReportName(newItem))
        createTime.set(newItem?.createTime.orEmpty())
    }

    fun onItemClick() {
        onItemClick.value?.invoke(null)
    }

    private fun buildReportName(data: ReportInfo?): String {
        return data?.reportName?.takeIf { it.isNotBlank() }
            ?: data?.title.orEmpty()
    }

    private fun buildCompletedReportName(data: ReportInfo?): String {
        val reportName = buildReportName(data)
        val companyName = data?.companyName.orEmpty()
        val jobTitle = data?.jobTitleStd.orEmpty()
        return when {
            reportName.isBlank() -> data?.title.orEmpty()
            companyName.isNotBlank() -> reportName + companyName
            jobTitle.isNotBlank() && data?.status == ReportStatus.OTHER -> reportName + jobTitle
            else -> reportName
        }
    }

    private fun buildFailReason(data: ReportInfo?): String {
        return data?.failReason?.takeIf { it.isNotBlank() }
            ?: R.string.report_failed_no_cost.getResString()
    }
}
