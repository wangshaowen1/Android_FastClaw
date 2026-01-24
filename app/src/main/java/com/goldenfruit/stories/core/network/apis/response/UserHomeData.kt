package com.kgmedia.fastfilm.core.network.apis.response


import android.os.Parcelable
import com.kgmedia.fastfilm.core.network.apis.entity.UserInfo
import kotlinx.parcelize.Parcelize

/**
 * 获取用户主页信息接口
 */
@Parcelize
data class UserHomeData(
    /**
     * 用户信息
     */
    val userInfo: UserInfo?,
    /**
     * 是否是注册，0-不是注册，1-是注册
     */
    val registerStatus: Int?
) : Parcelable {


    //新注册
    fun isNewRegistration(): Boolean {
        return registerStatus == 1
    }
}