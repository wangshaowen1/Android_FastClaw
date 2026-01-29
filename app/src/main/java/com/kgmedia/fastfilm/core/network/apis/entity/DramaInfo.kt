package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class DramaInfo(
    var id: String,//作品id
    var workName: String? = null,//作品名称
    var coverUrl: String? = null,

    var intro: String? = null,//作品简介
    var userId: String? = null,//作者用户ID
    var workStyleCode: String? = null,//作品风格code
    var workRatio: String? = null,//视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)
    var episodeCount: Int? = null,//集数
    var playCount: Int? = null,//播放总量(初始化1000-10000)
    var workStoryItemList: List<DramaEpisodeInfo>? = null,
) : Parcelable {

    val stripedTrickTitle
        get() = workName?.replace("\n", "")?.replace("\r", "")


    fun getDuring(index: Int = 0): Long? {
        return workStoryItemList?.getOrNull(index)?.duration
    }

    fun getVideoCoverUrl(): String? {
        return coverUrl
    }

    fun getPlayUrl(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.videoUrl
    }

    override fun toString(): String {
        return "DramaInfo(id=$id, name=$workName, playUrl=${getPlayUrl()} })"
    }

    fun equalsWithMedias(other: DramaInfo?): Boolean {
        if (this != other) return false
        if (id != other.id) return false

        return true

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DramaInfo

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**
 *  {
 *                         "id": "所属剧集ID",
 *                         "playCount": "播放量(初始化1000-10000)",
 *                         "videoUrl": "当前视频url",
 *                         "duration": "视频时长(毫秒)",
 *                         "workRatio": "视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)"
 *                     }
 */
@Parcelize
data class DramaEpisodeInfo(
    val duration: Long? = null,
    val id: String? = null,
    val playCount: Int? = null,
    val videoUrl: String? = null,
    val workRatio: String? = null
) : Parcelable