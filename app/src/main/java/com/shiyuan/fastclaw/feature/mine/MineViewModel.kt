package com.shiyuan.fastclaw.feature.mine

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Global
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.network.apis.response.SimpleBannerInfo
import com.shiyuan.fastclaw.core.network.apis.response.UserPointItemData
import com.shiyuan.fastclaw.core.response
import com.umeng.analytics.pro.da
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

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


    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.mine_sticks.getResString(),
        menuText = R.string.mine_rule.getResString(),
        background = Color.TRANSPARENT,
    )


    //是否登录
    val isLogin by lazy { MutableLiveData(false) }

    //用户资料Map
    val userInfo by lazy { ObservableField<Map<String, String?>>() }

    //没有登录不让进
    val noLogIn = MutableLiveData(false)

    /**
     * 账户余额
     */
    val balance by lazy { ObservableField<String>() }
    val workCourseBanner by lazy { ObservableField<SimpleBannerInfo>() }

    /**
     * 账户虾条
     */
    val userPonts by lazy { ObservableField<String>() }

    /**
     * 虾条记录
     */
    val userPointListFlow = Pager(PagingConfig(
        pageSize = 10,
        initialLoadSize = 10
    )) {
        UserPointPagingSource()
    }.flow
        .flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)

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
            isLogin.postValue(!user.isVisitor)
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
        getUserPoints()
    }

    /**
     * 获取用户主页信息接口
     */
    private fun getUserHomeInfo() {
        User.get().let { user ->
            isLogin.postValue(!user.isVisitor)
            if (user.isVisitor) {
                notLoggedIn()
                return
            }
            noLogIn.value = false
            Global.refreshBalance()
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
     * 获取积分信息(虾条)
     */
    private fun getUserPoints() {
        if (noLogIn.value == false) {
            launch {
                response(
                    bean = mRepo.api.getUserPoints(),
                    result = {
                        if (data == null) return@response
                        userPonts.set("${data.pointsBalance?.plus(100)}")
                    }
                )
            }
        }
    }


    private fun notLoggedIn() {
        userInfo.set(
            mapOf(
                "id" to "",
                "nickname" to "点击登录",
                "avatar" to "",
            )
        )
        noLogIn.value = true
        balance.set("0")
        userPonts.set("-")
        workCourseBanner.set(Config.get().appConfig.workCourseBanner)
    }

    private fun logOn() {
        val user = User.get()
        userInfo.set(
            mapOf(
                "id" to user.userUid,
                "nickname" to user.nickName,
                "avatar" to user.avatarUrl,
            )
        )
        balance.set(user.computingPower.toString())
        workCourseBanner.set(if (user.isPurchasedCourse()) null else Config.get().appConfig.workCourseBanner)
    }
}