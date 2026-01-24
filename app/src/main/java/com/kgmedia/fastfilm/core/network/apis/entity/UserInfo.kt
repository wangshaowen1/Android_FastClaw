package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 用户资料数据类
 * @author xiaoowuu
 */
@Parcelize
data class UserInfo(
    /** 头像URL */
    val avatarUrl: String? = null,

    /** 用户昵称 */
    val nickName: String? = null,

    /** 绑定的手机号 */
    var phoneBind: String?,

    /** 用户AppId */
    val userAppId: String? = null,

    /** 用户token，实际返回String类型 */
    val userToken: String? = null,

    /** 用户会员信息 */
    val vip: VipInfo? = null,

    /** 用户钱包信息 */
    val walletInfo: WalletInfo?,

    /** 城市 */
    val city: String? = null,

    /** 省份 */
    val province: String? = null,

    /** 性别 0-未知 1-男 2-女 */
    val gender: Int? = null,

    /** 生日时间戳 */
    val birthday: Long? = null,

    /** 个人简介 */
    val signature: String? = null,

    /** 创建时间（注册时间），实际返回时间戳 */
    val createTime: Long? = null,
) : Parcelable

/**
 * 用户会员信息数据类
 */
@Parcelize
data class VipInfo(
    /** 会员状态 0=非会员 1=会员 2-会员过期 */
    val vipStatus: Int? = null,

    /** 会员过期时间，实际返回时间戳 */
    val vipExpiredTime: Long? = null,

    /** vip类型：1-初级会员 2-中级会员 3-高级会员*/
    val vipType: Int? = null,
    /** vip类型对应的分成比例*/
    val profitSharingRatio: String? = null,
) : Parcelable