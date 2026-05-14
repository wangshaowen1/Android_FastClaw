package com.shiyuan.fastclaw.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** 用户积分信息
 * @createtime 2026年05月13日 18:57
 * @author TooWhiteT
 */
@Parcelize
data class UserPointsData(
    /**
     * 当前可用积分
     */
    val pointsBalance: Int? = null,

    /**
     * 总获得积分
     */
    val totalPoints: Int? = null,

    /**
     * 今日是否已签到
     */
    val signedToday: Boolean? = false
) : Parcelable

@Parcelize
data class UserPointPageData(
    val records: List<UserPointItemData>? = null,

    val total: Int? = null,

    val size: Int? = null,

    val current: Int? = null,

    val pages: Int? = null,
) : Parcelable

@Parcelize
data class UserPointItemData(

    /**
     * 记录id
     */
    val id: String? = null,

    /**
     * 描述
     */
    val businessDesc: String? = null,

    /**
     * 积分变动
     */
    val changePoints: String? = null,

    /**
     * 类型
     * ADD
     */
    val transactionType: String? = null,

    /**
     * 时间
     * 2026-05-12 10:49:55
     */
    val createTime: String? = ""
) : Parcelable {

    fun showTransaction(): String = when (transactionType) {
        "ADD" -> {
            "+$changePoints"
        }
        "SUBTRACT" -> {
            "-$changePoints"
        }
        else -> {
            changePoints ?: "-"
        }
    }

}
