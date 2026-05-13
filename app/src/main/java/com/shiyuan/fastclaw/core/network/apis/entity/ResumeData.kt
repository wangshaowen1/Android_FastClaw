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
    var blockingIssues: List<String>? = null,
    val resumeFileUrl: String ?,
    var checkStatus: String? = null,
    val resumeFileName: String ?,
    val resumeFileType: String ?,
    val submitTime: String ? = null,
    val checkId: String ? = null,
): Parcelable{


    val safeCheckStatus: String
        get() = checkStatus ?: "PASS"

}
