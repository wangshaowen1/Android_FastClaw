package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize
import java.util.regex.Pattern

@Parcelize
class TrackInfo(
    var artistsNote: String? = null,
    var createTime: Long? = null,
    var trackId: String,//作品id
    var trackTitle: String? = null,//作品标题
    val interaction: Interaction? = null,
    var music: MusicInfo? = null,
    val plays: Int? = null,//播放次数
    var sceneId: String? = null,
    /**
     * 发行状态：-1=未发行 0=发行中 1=已发行 2=发行失败
     */
    val issueStatus:Int?  = -1,
    //发行时间
    val buyTime: Long? = null,
    val issuePlatformIconList: List<String>?=null,

    val issueSuccessTime: Long? = null,//发行成功时间
    val selectedStatus:Int?=0
) : Parcelable {
    @Expose(serialize = false, deserialize = false)
    var isNative: Boolean = false

    val duration: Long? get() = music?.duration
    val lyric: String? get() = music?.lyric
    val lyricWithTime: String? get() = music?.lyricWithTime

    /**
     * 歌曲生成状态 0-创作中 1-创作成功完成 2-创作失败 3=初步生成完成 4=排队中
     */
    val musicGenerateStatus: Int? get() = music?.generateStatus
    val stripedTrickTitle
        get() = trackTitle?.replace("\n", "")?.replace("\r", "")

    val krcFirstTime: Long
        get() {
            if (music?.lyricWithTime.isNullOrEmpty()) {
                return 0L
            }
            // 使用正则表达式匹配第一个时间戳
            val timestampRegex = Pattern.compile("\\[(\\d+),")
            val matcher = timestampRegex.matcher(music!!.lyricWithTime!!)
            return if (matcher.find()) {
                matcher.group(1).toLong()
            } else {
                0L
            }
        }

    var recommenderNick: String? = null
    var recommenderId: String? = null

    fun isLike(): Boolean {
        return interaction?.likeStatus == 1
    }

    fun musicCoverUrl(): String? {
        return music?.coverUrl
    }

    fun hdMusicCoverUrl(): String? {
        return music?.largeCoverUrl ?: music?.coverUrl
    }
    fun musicPlayUrl(): String? {
        return music?.playUrl?:music?.pres?.find { it.playUrl?.isNotEmpty()==true }?.playUrl
    }

    fun isInstrumental(): Boolean {
        return music?.pureStatus == 1
    }

    override fun toString(): String {
        return "TrackInfo(trackId=$trackId, trackTitle=$trackTitle, play url=${musicPlayUrl()} })"
    }
    fun equalsWithMedias(other: TrackInfo?): Boolean {
        if (this != other) return false
        if (trackId != other.trackId) return false
        if (music != other.music) return false

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