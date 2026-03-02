package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import androidx.room.TypeConverters
import com.kgmedia.fastfilm.common.ext.toUnitText
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.db.convert.Converters
import kotlinx.parcelize.Parcelize

@Parcelize
@TypeConverters(Converters::class)
data class WorkInfo(
    //作品ID
    val workId: String? = null,
    //作品名称
    val workTitle: String? = null,
    //作品简介
    val workDesc: String? = null,
    //作品风格code
    val workStyleCode: String? = null,
    //视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)
    val workRatio: String? = null,
    //作品灵感
    val workInspiration: String? = null,
    //作品封面图URL（AI生成或默认图）
    val workCoverUrl: String? = null,
    //作品类型：1-灵感创作;2-小说改编;3-小说上传
    val workType: Int? = null,
    //作品状态：草稿（20=剧本生成中 30=资产生成中） 生成中（40=分镜生成中  42=分镜视频生成中 50=待合成） 60=已完成 70=失败
    val workStatus: Int? = null,
    //总集数（规划集数）
    val totalEpisodeCnt: Int? = null,
    //已完成集数
    val finishedEpisodeCnt: Int? = null,
    //最近生成的集数编号
    val lastEpisodeNo: Int? = null,
    //最近一次生成时间
    val lastGenerateTime: String? = null,
    //播放量(初始化1000-10000)
    val playCount: Int? = null,
    //作者用户ID
    var userId: String? = null,
    //剧集
    var workStoryItemList: List<EpisodeInfo>? = null,
    /**[枚举]签约状态(0-未申请 1-审核中 2-已通过 3-已驳回)*/
    val contractStatus: Int? = null,
    /**[枚举]发行状态(0-未发行 1-待签约 2-发行上架中 3-已发行)*/
    val publishStatus: Int? = null,
    /**驳回原因*/
    val rejectReason: String? = null,
    /**签约评级（S/A/B/C等）*/
    val contractLevel: String? = null,
    //[枚举]小说上传模式下-第一集是否有剧本(0-没有 1-有)
    val novelUploadIsOneStoryContentStatus: Int? = null,
    /**发行平台图片 list*/
    val publishPlatformImageList: List<String>? = null
) : Parcelable {

    companion object {
        //作品类型：1-灵感创作
        const val INSPIRATION_CREATION = 1

        //作品类型：2-小说创作
        const val NOVEL_CREATION = 2

        //作品类型：3-小说上传
        const val NOVEL_UPLOAD = 3

        //剧本生成中
        const val DRAFT_GEN_STATE = 20

        //资产生成中
        const val DRAFT_STATE = 30

        //分镜生成中
        const val GEN_MUSIC_BASE = 40

        //分镜视频生成中
        const val GEN_MUSIC_VIDEO = 42

        //待合成状态
        const val WAIT_STATE = 50

        //已完成状态
        const val COMPLETED_STATE = 60

        //失败状态
        const val FAILED_STATE = 70
    }


    /**
     * 小说上传模式下-第一集是否有剧本
     * @return Boolean
     */
    fun isInspirationUploadMode(): Boolean {
        return novelUploadIsOneStoryContentStatus == 1
    }


    //是否是生成中状态
    fun isGenerating(): Boolean {
        return workStatus == GEN_MUSIC_VIDEO
    }

    //草稿状态
    fun isDraftStatus(): Boolean {
        return workStatus in listOf(DRAFT_GEN_STATE, DRAFT_STATE, GEN_MUSIC_BASE)
    }

    fun seriesIntroduction(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.storySummary
    }


    fun isInspirationGenerated(): Boolean {
        return workType == 1
    }


    fun isMine(): Boolean {
        return User.get().isSameId(userId)
    }

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

    fun getEpisodeId(index: Int = 0): String? {
        return workStoryItemList?.getOrNull(index)?.storyId
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
 *                 "stepProgress": "[枚举]创作流程进度 [10=剧本输入 20=资产生成中 30=分镜生成中 40=已完成  35=待合成]",
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
    //剧集id
    val storyId: String? = null,
    //剧集标题
    val storyTitle: String? = null,
    //剧集梗概
    val storySummary: String? = null,
    //剧集剧本内容
    val storyContent: String? = null,
    //序号
    val sortNo: Int? = null,
    //[枚举]创作流程进度 [10=剧本输入 15=剧本生成中 20=资产生成中 30=分镜生成中 32=分镜视频生成中 35=待合成 40=已完成]
    val stepProgress: Int? = null,
    //[枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败]
    val stepStatus: Int? = null,
    //分镜数
    val storyboardTotal: Int? = null,
    //当前视频url
    val videoUrl: String? = null,
    //视频时长(毫秒)
    val duration: Long? = null,
    //视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)
    val workRatio: String? = null,
    //播放量(初始化1000-10000)
    val playCount: Int? = null,
    //续创剧集id（灵感续创才有的，小说没有，保存剧本需要带回来）
    val sequelStoryId: String? = null,
    //封面
    val workCoverUrl: String? = null
) : Parcelable {

    companion object {
        //剧本输入
        const val PROGRESS_DRAFT = 10

        //剧本生成中
        const val PROGRESS_STORY_GENERATING = 15

        //资产生成中
        const val PROGRESS_ASSET_GENERATING = 20

        //分镜生成中
        const val PROGRESS_STORYBOARD_GENERATING_ASSET = 30

        //分镜视频生成中
        const val PROGRESS_STORYBOARD_GENERATING = 32

        //待合成
        const val PROGRESS_WAITING_COMPOSING = 35

        //已完成
        const val PROGRESS_COMPLETED = 40
    }


    /**
     * 是否是正在生成中
     * @return Boolean
     */
    fun isGenerating(): Boolean {
        return stepProgress == PROGRESS_STORYBOARD_GENERATING
    }

    /**
     * 等待合成
     * @return Boolean
     */
    fun isWaitingComposing(): Boolean {
        return stepProgress == PROGRESS_WAITING_COMPOSING
    }

    /**
     * 已完成
     * @return Boolean
     */
    fun isCompleted(): Boolean {
        return stepProgress == PROGRESS_COMPLETED
    }

    /**
     * 是否是草稿
     * @return Boolean
     */
    fun isDraft(): Boolean {
        return stepProgress in listOf(
            PROGRESS_DRAFT,
            PROGRESS_STORY_GENERATING,
            PROGRESS_ASSET_GENERATING,
            PROGRESS_STORYBOARD_GENERATING_ASSET
        )
    }
}