package com.shiyuan.fastclaw.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class CommonEnumItem(
    val code: String? = null,
    val name: String? = null,
    val enabled: Boolean? = null,
    val sortOrder: Int? = null,
)


@Parcelize
data class CommonItems(
    var fieldCode: String? = null,
    var fieldName: String? = null,
    var fieldType: String? = null,
    var fieldValue: String? = null,
    var fieldItems: List<CommonItems> = emptyList(),
): Parcelable {}