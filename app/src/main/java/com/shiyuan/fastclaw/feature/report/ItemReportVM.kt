package com.shiyuan.fastclaw.feature.report

import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

/**
 * 报告列表项 ViewModel
 */
class ItemReportVM(data: ReportInfo?) : AppItemViewModel<ReportInfo>(data) {

    val isFailed = ObservableField(data?.status == ReportStatus.FAILED)
    val isInProgress = ObservableField(data?.status == ReportStatus.IN_PROGRESS)
    val isOther = ObservableField(data?.status == ReportStatus.OTHER)

    val estimateMessage = ObservableField(data?.estimateMessage.orEmpty())
    val failReason = ObservableField(data?.failReason.orEmpty())

    val progress = ObservableField(data?.progress)

    val jobTitleStd = ObservableField("目标岗位：" + data?.jobTitleStd.orEmpty())

    val isJobTitleStd = ObservableField(data?.jobTitleStd.isNullOrBlank())

    val reportNameCompanyName = ObservableField(if (data?.companyName.isNullOrBlank()) data?.reportName.orEmpty() else data?.companyName.orEmpty())

    val stTime = ObservableField(if(data?.jobTitleStd.isNullOrBlank()) "上传简历于 ${data?.createTime}" else "上传简历+JD于：${data?.createTime}" )

    val progressBgC = ObservableField(getScoreProgressColor(data?.totalScore))

    val progressBg = ObservableField(getScoreProgressBgColor(data?.totalScore))


    val textC = ObservableField(getScoreTextColor(data?.totalScore))

   val textBg = ObservableField(getScoreTextBgColor(data?.totalScore))


  //  val textBg: Int get() = getScoreTextBgColor(data?.get()?.totalScore)  // 复用你的函数

    val tex = ObservableField(getScoreText(data?.totalScore))

    val totalScore = ObservableField(data?.totalScore)


    val reportName = ObservableField(data?.reportName.orEmpty())

    val companyNameText = ObservableField(data?.companyName.orEmpty())
    val positionText = ObservableField(
        if (!data?.position.isNullOrBlank()) "目标岗位：${data?.position}" else ""
    )

    val tagText = ObservableField(data?.tag.orEmpty())
    val tagVisible = ObservableField(!data?.tag.isNullOrEmpty())

    override fun updateData(newItem: ReportInfo?) {
        super.updateData(newItem)
        isFailed.set(newItem?.status == ReportStatus.FAILED)
        isInProgress.set(newItem?.status == ReportStatus.IN_PROGRESS)
        isOther.set(newItem?.status == ReportStatus.OTHER)
        estimateMessage.set(newItem?.estimateMessage.orEmpty())
        failReason.set(newItem?.failReason.orEmpty())
        progress.set(newItem?.progress)
        jobTitleStd.set("目标岗位：" + newItem?.jobTitleStd.orEmpty())
        isJobTitleStd.set(newItem?.jobTitleStd.isNullOrBlank())
        reportNameCompanyName.set(
            if (newItem?.companyName.isNullOrBlank()) newItem?.reportName.orEmpty() else newItem?.companyName.orEmpty()
        )
        stTime.set(if (newItem?.jobTitleStd.isNullOrBlank()) "上传简历于 ${newItem?.createTime}" else "上传简历+JD于：${newItem?.createTime}")
        progressBgC.set(getScoreProgressColor(newItem?.totalScore))
        progressBg.set(getScoreProgressBgColor(newItem?.totalScore))
        textC.set(getScoreTextColor(newItem?.totalScore))
        textBg.set(getScoreTextBgColor(newItem?.totalScore))
        tex.set(getScoreText(newItem?.totalScore))
        totalScore.set(newItem?.totalScore)
        reportName.set(newItem?.reportName.orEmpty())
        companyNameText.set(newItem?.companyName.orEmpty())
        positionText.set(if (!newItem?.position.isNullOrBlank()) "目标岗位：${newItem?.position}" else "")
        tagText.set(newItem?.tag.orEmpty())
        tagVisible.set(!newItem?.tag.isNullOrEmpty())
    }




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

    private fun getScoreTextColor(score: Int?): Int {
        return when {
            score == null -> 0xFFFF5967.toInt()
            score >= 80 -> 0xFF0095FF.toInt()
            score >= 50 -> 0xFFFFEE00.toInt()
            else -> 0xFFFF5967.toInt()
        }
    }

    private fun getScoreTextBgColor(score: Int?): Int {
        return when {
            score == null ->  R.drawable.bg_1aff5967_r4
            score >= 80 -> R.drawable.bg_1a0095ff_r4
            score >= 50 ->  R.drawable.bg_1affee00_r4
            else -> R.drawable.bg_1aff5967_r4
        }
    }



    private fun getScoreText(score: Int?): String {
        return when {
            score == null -> "新手小白"
            score >= 98 -> "offer海王"
            score >= 88 -> "天作牛马"
            score >= 75 -> "潜力黑马"
            score >= 65 -> "潜力选手"
            score >= 50 -> "基础达标"
            score >= 38 -> "新手小白"
            else -> "新手小白"
        }
    }
}

