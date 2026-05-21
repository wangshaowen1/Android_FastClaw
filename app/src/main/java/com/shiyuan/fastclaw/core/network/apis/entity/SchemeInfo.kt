package com.shiyuan.fastclaw.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SchemeData(
    val scheme: String?,
    var code: String?,
    var url: String?,
) : Parcelable {

}