package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import com.kgmedia.fastfilm.common.ext.toUnitText
import kotlinx.parcelize.Parcelize

@Parcelize
class WorkInfo(
    var workId: String,//作品id
    var workTitle: String? = null,//作品名称
    var workCoverUrl: String? = null,
    var workDesc: String? = null,//作品简介
    var userId: String? = null,//作者用户ID
    var workStyleCode: String? = null,//作品风格code
    var workRatio: String? = null,//视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)
    var totalEpisodeCnt: Int? = null,//总集数（规划集数）
    var playCount: Int? = null,//播放总量(初始化1000-10000)
    var workStoryItemList: List<EpisodeInfo>? = null,

    var workInspiration: String? = null,//作品灵感
    var workType: String? = null,//作品类型：1-灵感创作 2-小说改编
    var workStatus: String? = null,//作品状态：草稿（30=资产生成中） 生成中（40=分镜生成中  42=分镜视频生成中 50=合成中） 60=已完成 70=失败
    var finishedEpisodeCnt: String? = null,//已完成集数
    var lastEpisodeNo: String? = null,//最近生成的集数编号
    var lastGenerateTime: String? = null,//最近一次生成时间
) : Parcelable {

    val stripedTrickTitle
        get() = workTitle?.replace("\n", "")?.replace("\r", "")

    fun getDuring(index: Int = 0): Long? {
        return workStoryItemList?.getOrNull(index)?.duration
    }

    fun getVideoCoverUrl(): String? {
        return workCoverUrl
    }

    fun getPlayUrl(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.videoUrl
    }

    fun getFormatPlaycount(): String? {
        return playCount?.toUnitText()
    }

    override fun toString(): String {
        return "DramaInfo(id=$workId, name=$workTitle, playUrl=${getPlayUrl()} })"
    }

    fun equalsWithMedias(other: WorkInfo?): Boolean {
        if (this != other) return false
        if (workId != other.workId) return false

        return true

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkInfo

        return workId == other.workId
    }

    override fun hashCode(): Int {
        return workId.hashCode()
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
 *                     {
 *                 "storyId": "剧集id",
 *                 "storyTitle": "剧集标题",
 *                 "storySummary": "剧集梗概",
 *                 "storyContent": "剧集剧本内容",
 *                 "sortNo": "序号",
 *                 "stepProgress": "[枚举]创作流程进度 [10=剧本输入 20=资产生成中 30=分镜生成中 40=已完成]",
 *                 "stepStatus": "[枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败]",
 *                 "storyboardTotal": "分镜数",
 *                 "videoUrl": "当前视频url",
 *                 "duration": "视频时长(毫秒)",
 *                 "workRatio": "视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)",
 *                 "playCount": "播放量(初始化1000-10000)"
 *             }
 */
@Parcelize
data class EpisodeInfo(
    val duration: Long? = null,
    val storyId: String? = null,
    val playCount: Int? = null,
    val videoUrl: String? = null,
    val workRatio: String? = null,

    val storyTitle: String? = null,
    val storySummary: String? = null,
    val storyContent: String? = null,
    val sortNo: String? = null,
    val stepProgress: String? = null,
    val stepStatus: String? = null,
    val storyboardTotal: String? = null,
) : Parcelable