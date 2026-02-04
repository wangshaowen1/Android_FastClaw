package com.kgmedia.fastfilm.core.network.apis.response

import android.os.Parcelable
import androidx.room.TypeConverters
import com.kgmedia.fastfilm.core.db.convert.Converters
import kotlinx.parcelize.Parcelize

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品信息
 * @Data: 2026/2/3/周二  9:46
 * @Version: 1.0
 * *****************************************
 */
@Parcelize
@TypeConverters(Converters::class)
data class WorkInformationData(
    //作品ID
    val workId: String?,
    //作品名称
    val workTitle: String?,
    //作品简介
    val workDesc: String?,
    //作品风格code
    val workStyleCode: String?,
    //视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)
    val workRatio: String?,
    //作品灵感
    val workInspiration: String?,
    //作品封面图URL（AI生成或默认图）
    val workCoverUrl: String?,
    //作品类型：1-灵感创作 2-小说改编
    val workType: Int?,
    //作品状态：草稿（30=资产生成中） 生成中（40=分镜生成中 42=分镜视频生成中 50=合成中） 60=已完成 70=失败
    val workStatus: Int?,
    //总集数（规划集数）
    val totalEpisodeCnt: Int?,
    //已完成集数
    val finishedEpisodeCnt: Int?,
    //最近生成的集数编号
    val lastEpisodeNo: Int?,
    //最近一次生成时间
    val lastGenerateTime: Int?,
    //播放量(初始化1000-10000)
    val playCount: Int?,
) : Parcelable {
    companion object {
        //草稿状态
        const val DRAFT_STATE = 30

        //待合成状态
        const val WAIT_STATE = 1002

        //分镜生成中
        const val GEN_MUSIC_BASE = 40

        //分镜视频生成中
        const val GEN_MUSIC_VIDEO = 42

        //合成中状态
        const val COMPOSING_STATE = 50

        //已完成状态
        const val COMPLETED_STATE = 60

        //失败状态
        const val FAILED_STATE = 70
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkInformationData

        if (workType != other.workType) return false
        if (workStatus != other.workStatus) return false
        if (totalEpisodeCnt != other.totalEpisodeCnt) return false
        if (finishedEpisodeCnt != other.finishedEpisodeCnt) return false
        if (lastEpisodeNo != other.lastEpisodeNo) return false
        if (lastGenerateTime != other.lastGenerateTime) return false
        if (playCount != other.playCount) return false
        if (workId != other.workId) return false
        if (workTitle != other.workTitle) return false
        if (workDesc != other.workDesc) return false
        if (workStyleCode != other.workStyleCode) return false
        if (workRatio != other.workRatio) return false
        if (workInspiration != other.workInspiration) return false
        if (workCoverUrl != other.workCoverUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = workType ?: 0
        result = 31 * result + (workStatus ?: 0)
        result = 31 * result + (totalEpisodeCnt ?: 0)
        result = 31 * result + (finishedEpisodeCnt ?: 0)
        result = 31 * result + (lastEpisodeNo ?: 0)
        result = 31 * result + (lastGenerateTime ?: 0)
        result = 31 * result + (playCount ?: 0)
        result = 31 * result + (workId?.hashCode() ?: 0)
        result = 31 * result + (workTitle?.hashCode() ?: 0)
        result = 31 * result + (workDesc?.hashCode() ?: 0)
        result = 31 * result + (workStyleCode?.hashCode() ?: 0)
        result = 31 * result + (workRatio?.hashCode() ?: 0)
        result = 31 * result + (workInspiration?.hashCode() ?: 0)
        result = 31 * result + (workCoverUrl?.hashCode() ?: 0)
        return result
    }
}