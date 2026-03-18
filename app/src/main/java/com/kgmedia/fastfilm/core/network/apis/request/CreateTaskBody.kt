package com.kgmedia.fastfilm.core.network.apis.request

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreateTaskBody(
    val workInspiration: String? = null,
    val content: String? = null,
    val workTitle: String? = null,
    val totalEpisodeCntCode: String? = null,
    val totalEpisodeCnt: String? = null,
    val workRatio: String? = null,
    val workStyleCode: String? = null,
) : Parcelable
