package com.shiyuan.fastclaw.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
  简历数据
 */

@Parcelize
 class ResumeData(
    val hasLatestResume: Boolean = false,
    var isJd: Boolean = true,
    var blockingIssuesMessage: String? = null,
    var blockingMessage: String? = null,
    var blockingIssues: List<String>? = null,
    val resumeFileUrl: String ?,
    var checkStatus: String? = null,
    var resumeFileName: String ?,
    val resumeFileType: String ?,
    val submitTime: String ? = null,
    var checkId: String ? = null,
): Parcelable{


    val safeCheckStatus: String
        get() = checkStatus ?: "PASS"


    fun copy(
        hasLatestResume: Boolean = this.hasLatestResume,
        isJd: Boolean = this.isJd,
        blockingIssuesMessage: String? = this.blockingIssuesMessage,
        blockingMessage: String? = this.blockingMessage,
        blockingIssues: List<String>? = this.blockingIssues,
        resumeFileUrl: String? = this.resumeFileUrl,
        checkStatus: String? = this.checkStatus,
        resumeFileName: String? = this.resumeFileName,
        resumeFileType: String? = this.resumeFileType,
        submitTime: String? = this.submitTime,
        checkId: String? = this.checkId
    ) = ResumeData(
        hasLatestResume, isJd, blockingIssuesMessage, blockingMessage,
        blockingIssues, resumeFileUrl, checkStatus, resumeFileName,
        resumeFileType, submitTime, checkId
    )
}


@Parcelize
class ReportData(
    val reportStatus: String ?,
    val reportId: String ?,
): Parcelable{

}
