package com.shiyuan.fastclaw.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *                         "platformCode": "平台code",
 *                         "platformName": "平台名称",
 *                         "platformLogoUrl": "平台logo URL",
 *                         "h5Url": "作品在该平台的H5跳转链接，为空则不可跳转"
 *                     }
 */
@Parcelize
data class PlatformInfo(
    val h5Url: String? = null,
    val platformCode: String? = null,
    val platformLogoUrl: String? = null,
    val platformName: String? = null
) : Parcelable