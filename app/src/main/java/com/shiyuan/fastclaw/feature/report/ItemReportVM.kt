package com.shiyuan.fastclaw.feature.report

import androidx.databinding.ObservableField
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

/**
 * 报告列表项 ViewModel
 */
class ItemReportVM(data: ReportInfo?) : AppItemViewModel<ReportInfo>(data) {

    val isGenerating = ObservableField(data?.status == ReportStatus.GENERATING)
    val isCompleted = ObservableField(data?.status == ReportStatus.COMPLETED)

    val scoreText = ObservableField(data?.score?.toString().orEmpty())
    val titleText = ObservableField(data?.title.orEmpty())

    val companyNameText = ObservableField(data?.companyName.orEmpty())
    val positionText = ObservableField(
        if (!data?.position.isNullOrBlank()) "目标岗位：${data?.position}" else ""
    )

    val dateText = ObservableField(
        if (!data?.date.isNullOrBlank()) "上传简历于 ${data?.date}" else ""
    )

    val tagText = ObservableField(data?.tag.orEmpty())
    val tagVisible = ObservableField(!data?.tag.isNullOrEmpty())

    val progressText = ObservableField(data?.progressText.orEmpty())
    val progressVisible = ObservableField(data?.status == ReportStatus.GENERATING)

    val circleStrokeColorRes = ObservableField(getScoreStrokeColorRes(data?.score))
    val circleProgressColorRes = ObservableField(getScoreProgressColorRes(data?.score))
    val circleBgColorRes = ObservableField(R.color.color_10_000000)

    fun onItemClick() {
        onItemClick.value?.invoke(null)
    }

    private fun getScoreStrokeColorRes(score: Int?): Int {
        return R.color.color_10_000000
    }

    private fun getScoreProgressColorRes(score: Int?): Int {
        return when {
            score == null -> R.color.func_success_dark
            score >= 70 -> R.color.func_success_dark
            score >= 40 -> R.color.func_warning_dark
            else -> R.color.func_failure_dark
        }
    }
}
