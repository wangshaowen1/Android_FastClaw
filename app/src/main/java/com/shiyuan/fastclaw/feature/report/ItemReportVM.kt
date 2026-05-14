package com.shiyuan.fastclaw.feature.report

import androidx.databinding.ObservableField
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

/**
 * 报告列表项 ViewModel
 */
class ItemReportVM(data: ReportInfo?) : AppItemViewModel<ReportInfo>(data) {

    val isGenerating = ObservableField(data?.status == ReportStatus.IN_PROGRESS)

    val estimateMessage = ObservableField(data?.estimateMessage.orEmpty())

    val progress = ObservableField(data?.progress)

    val jobTitleStd = ObservableField("目标岗位："+ data?.jobTitleStd.orEmpty())

    val isJobTitleStd = ObservableField(data?.jobTitleStd.isNullOrBlank())

    val reportNameCompanyName = ObservableField(if (data?.companyName.isNullOrBlank()) data?.reportName.orEmpty() else data?.companyName.orEmpty())

    val stTime = ObservableField(if(data?.jobTitleStd.isNullOrBlank()) "上传简历于 ${data?.createTime}" else "上传简历+JD于：${data?.createTime}" )

    val progressBgC = ObservableField(getScoreProgressColor(data?.totalScore))

    val progressBg = ObservableField(getScoreProgressBgColor(data?.totalScore))

    val totalScore = ObservableField(data?.totalScore)


    val reportName = ObservableField(data?.reportName.orEmpty())

    val companyNameText = ObservableField(data?.companyName.orEmpty())
    val positionText = ObservableField(
        if (!data?.position.isNullOrBlank()) "目标岗位：${data?.position}" else ""
    )

    val tagText = ObservableField(data?.tag.orEmpty())
    val tagVisible = ObservableField(!data?.tag.isNullOrEmpty())



    fun onItemClick() {
        onItemClick.value?.invoke(null)
    }

    private fun getScoreProgressColor(score: Int?): Int {
        return when {
            score == null -> 0xFFFF5967.toInt()
            score >= 80 -> 0xFF0095FF.toInt()
            score >= 50 -> 0xFFFFEE00.toInt()
            else -> 0xFFFF5967.toInt()
        }
    }

    private fun getScoreProgressBgColor(score: Int?): Int {
        return when {
            score == null -> 0x1AFF5967.toInt()
            score >= 80 -> 0x1A0095FF.toInt()
            score >= 50 -> 0x1AFFEE00.toInt()
            else -> 0x1AFF5967.toInt()
        }
    }
}

