package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import androidx.room.Ignore
import com.kgmedia.fastfilm.core.network.apis.response.MusicPre
import kotlinx.parcelize.Parcelize

@Parcelize
class MusicInfo(

    var coverUrl: String?,
    val largeCoverUrl: String?,
    var duration: Long?,
    /**
     * 歌曲生成状态 0-创作中 1-创作成功完成 2-创作失败 3=初步生成完成 4=排队中
     */
    var generateStatus: Int?,
    var lyric: String?,
    var lyricWithTime: String?,
    var playUrl: String?,
    var playUrlWav: String?,
    var prompt: String?,
    /**
     * 制作同款灵感词,非灵感写歌使用
     */
    var generateSamePrompt: String?,
    var promptImage: String?,
    var pureStatus: Int?,

    var queueNumber: Int?,
    /**
     * 音乐生成方式 1=灵感写歌 2=自定义写歌 3=图片写歌 4=场景写歌 5=出道神曲 =9-视频换脸_送他歌 10-视频换脸_灵感写歌
     */
    var generateWay: Int? = null,

    /**
     * open-ai的API的类型 1-suno 2-多米 3-天工 4-aceData 10001-豆包
     */
    var openApiType: Int? = null,
    /**
     * 音乐生成配置表id
     */
    var musicGenConfigId: String? = null,

    //发行权益集合（此歌曲购买过发行权益此字段才会返回值）
    val issueRights: List<IssueRights>? = null,
    //音乐生成成功时间
    val genSuccTime: Long? = null,
) : Parcelable {


    @Ignore
    var pres: List<MusicPre>? = null//试听音乐
    

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MusicInfo

        if (coverUrl != other.coverUrl) return false
        if (playUrl != other.playUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coverUrl?.hashCode() ?: 0
        result = 31 * result + (playUrl?.hashCode() ?: 0)
        return result
    }

}