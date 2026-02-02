package com.kgmedia.fastfilm.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *         "storyId": "剧集 id",
 *         "workId": "作品 ID"
 *     }
 */
data class WorkIdData(
    val storyId: String? = null,
    val workId: String? = null
)