package com.kgmedia.fastfilm.feature.mine

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.aicompose.generating.GeneratingViewModel
import com.nhsd.basic.ext.getDrawable

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:10
 * @Version: 1.0
 * *****************************************
 */
class MineViewModel : AppRepositoryViewModel<AppRepository>() {


    val showBottomSpace = false

    //是否登录
    val isLogin by lazy { ObservableBoolean(false) }

    //用户资料Map
    val userInfo by lazy { ObservableField<Map<String, String?>>() }

    //是否是会员
    val isVip by lazy { ObservableBoolean(false) }
    val isPermanentVip by lazy { ObservableBoolean(false) }

    //没有登录不让进
    val noLogIn = MutableLiveData(false)

    //会员权益图标
    val vipLevelImg = ObservableField<Drawable?>(R.drawable.ic_mine_vip.getDrawable())


    //会员说明
    val vipDesc = ObservableField("")

    //剩余金额
    val availableBalance = ObservableField("")

    //会员状态
    val vipStatus = ObservableInt(0)

    val isMale = ObservableBoolean()

    //剧集
    val episode = MutableLiveData(0)

    //草稿
    val shortCount = MutableLiveData(0)

    //追剧
    val bingeWatch = MutableLiveData(0)

    //喜欢
    val like = MutableLiveData(0)

    override fun onCreate() {
        super.onCreate()
        refreshUserInfo()
        //更新必要信息
        updateInformation()
    }

    /**
     * 刷新用户信息
     */
    fun refreshUserInfo() {
        User.get().let { user ->
            isLogin.set(user.isLogged)
            if (user.isVisitor) {
                notLoggedIn()
                return
            }
            logOn()
        }
    }


    /**
     * 更新必要信息
     */
    fun updateInformation() {
        //获取用户主页信息接口
        getUserHomeInfo()
        //查询我的作品数量统计信息
        quantityStatisticsOfWorks()
    }

    /**
     * 获取用户主页信息接口
     */
    private fun getUserHomeInfo() {
        User.get().let { user ->
            isLogin.set(user.isLogged)
            if (user.isVisitor) {
                notLoggedIn()
                return
            }
            noLogIn.value = false
            launch {
                response(
                    bean = mRepo.api.getUserHomeInfo(UserTokenBody()),
                    result = {
                        if (data == null) return@response
                        User.get().updateFrom(data)
                        logOn()
                    })
            }
        }
    }

    /**
     * 查询我的作品数量统计信息
     */
    private fun quantityStatisticsOfWorks() {
        if (User.get().isVisitor) {
            return
        }
        launch {
            response(
                bean = mRepo.api.quantityStatisticsOfWorks(UserTokenBody()),
                result = {
                    data?.apply {
                        episode.value = dramasSeriesNum
                        shortCount.value = dramasScriptNum
                        bingeWatch.value = dramasSeriesWatchingNum
                        like.value = dramasScriptLikeNum
                    }
                }
            )
        }
    }

    private fun notLoggedIn() {
        noLogIn.value = true
        isVip.set(false)
        isPermanentVip.set(false)
        vipStatus.set(0)
        setVipType()
        userInfo.set(
            mapOf(
                "id" to "",
                "nickname" to "登录注册》",
                "avatar" to "",
            )
        )
        vipDesc.set("多项AI特权 ｜ 海量爆款内容")
        availableBalance.set("0")
    }

    private fun logOn() {
        User.get().let { user ->
            isVip.set(user.isVip)
            isPermanentVip.set(user.vipType == 3)
            vipStatus.set(user.vipStatus)
            setVipType()
            userInfo.set(
                mapOf(
                    "id" to user.appId,
                    "nickname" to user.nickname,
                    "avatar" to user.avatar,
                    "gender" to user.gender.toString(),
                    "generationLabel" to getGenerationLabel(user.birthday),
                    "city" to user.city,
                    "signature" to (user.signature.let { if (it.isNullOrEmpty()) "你还没有简介~" else it })
                )
            )
            if (user.isVip) {
                vipDesc.set("会员有效期至：${DateUtil.format(user.vipExpiredTime ?: 0L, DateUtil.FORMAT_DATE)}")
            } else {
                vipDesc.set("多项AI特权 ｜ 海量爆款内容")
            }
            isMale.set(user.gender == User.MALE)
            val amountOfMoney = user.scoreBalance
            if (amountOfMoney <= 0) {
                availableBalance.set("0")
            } else {
                availableBalance.set(amountOfMoney.toString())
            }
        }
    }

    private fun getGenerationLabel(birthDate: String?): String {
        return birthDate?.split("-")?.firstOrNull()?.getOrNull(2)?.let { decade ->
            "${decade}0后"
        } ?: ""
    }

    private fun setVipType() {
        User.get().let { user ->
            if (user.isVisitor) {
                vipLevelImg.set(null)
                return
            }
            if (!user.isVip) {
                vipLevelImg.set(null)
                return
            }
            vipLevelImg.set(R.drawable.ic_mine_vip.getDrawable())
        }
    }

}