package com.shiyuan.fastclaw.core.network.apis.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * 邀请统计数据
 */
@Parcelize
data class InviteStatsData(
    /**
     * 统计人数
     */
    val successInviteCount: Int? = null,

    /**
     * 总虾条
     */
    val totalRewardScore: Int? = null
) : Parcelable

/**
 * 邀请记录分页数据
 */
@Parcelize
data class InviteRecordPageData(
    val items: List<InviteRecordInfo>? = null,
    val total: Long? = null,
    val pageSize: Long? = null,
    val pageNum: Long? = null,
    val totalPage: Long? = null
) : Parcelable

/**
 * 邀请记录
 */
@Parcelize
data class InviteRecordInfo(
    /**
     * 邀请记录业务 ID
     */
    val inviteRecordId: Long? = null,

    /**
     * 被邀请用户昵称快照
     */
    val inviteeNickName: String? = null,

    /**
     * 邀请日期，格式 yyyy-MM-dd HH:mm:ss
     */
    val inviteDate: String? = null,

    /**
     * 邀请积分，记录邀请人通过当前被邀请人累计获得的积分
     */
    val inviteScore: Int? = null,

    /**
     * 注册状态，固定为 1
     */
    val registerStatus: Int? = null,

    /**
     * 注册状态文案
     */
    val registerStatusText: String? = null,

    /**
     * 体验状态：0-未体验，1-已体验
     */
    val experienceStatus: Int? = null,

    /**
     * 体验状态文案
     */
    val experienceStatusText: String? = null,

    /**
     * 首次体验工具名称
     */
    val experienceToolName: String? = null
) : Parcelable
