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
    var fileName: String ? = null,
    var resumeName: String ? = null,
    var updateTime: String ? = null,
    var resumeUrl: String ? = null,
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
        checkId: String? = this.checkId,
        fileName: String? = this.fileName,
        resumeName: String? = this.resumeName,
         updateTime: String? = this.updateTime
    ) = ResumeData(
        hasLatestResume, isJd, blockingIssuesMessage, blockingMessage,
        blockingIssues, resumeFileUrl, checkStatus, resumeFileName,
        resumeFileType, submitTime, checkId,fileName,resumeName,updateTime
    )
}


@Parcelize
class ReportData(
    val reportStatus: String ?,
    val reportId: String ?,
): Parcelable{

}
