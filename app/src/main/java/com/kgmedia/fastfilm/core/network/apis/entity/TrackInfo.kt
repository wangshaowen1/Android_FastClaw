package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class TrackInfo(
    var trackId: String,//作品id
    var trackTitle: String? = null,//作品标题
    var playUrl: String? = null,//作品标题
    var coverUrl: String? = null,//作品标题
) : Parcelable {

    val stripedTrickTitle
        get() = trackTitle?.replace("\n", "")?.replace("\r", "")

    fun musicCoverUrl(): String? {
        return coverUrl
    }

    fun musicPlayUrl(): String? {
        return playUrl
    }

    override fun toString(): String {
        return "TrackInfo(trackId=$trackId, trackTitle=$trackTitle, play url=${musicPlayUrl()} })"
    }

    fun equalsWithMedias(other: TrackInfo?): Boolean {
        if (this != other) return false
        if (trackId != other.trackId) return false
        if (playUrl != other.playUrl) return false

        return true

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackInfo

        return trackId == other.trackId
    }

    override fun hashCode(): Int {
        return trackId.hashCode()
    }
}