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


@Parcelize
data class ContinueTaskBody(
    val workInspiration: String? = null,//作品灵感（小说内容）小说续写得时候传
    val workId: String? = null,
    val storyId: String? = null,
    val workType: Int? = null,//作品类型：1-灵感创作;2-小说改编;
) : Parcelable
