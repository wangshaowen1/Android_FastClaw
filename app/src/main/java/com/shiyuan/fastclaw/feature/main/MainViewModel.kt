package com.shiyuan.fastclaw.feature.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.StatusCode
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Global
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.response
import com.tulele.im.ImSession
import com.tulele.im.utils.AccountHelper
import kotlin.text.get
import com.shiyuan.fastclaw.core.ext.request

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/4/17  9:38
 * @Version: 1.0
 * *****************************************
 */
class MainViewModel : AppRepositoryViewModel<AppRepository>() {

    //显示隐藏底部
    val showBottomBar = ObservableBoolean(true)

    //是否登录
    val isLogin by lazy { ObservableBoolean(false) }
    val fromPage by lazy { mBundle.getString(Constants.Extra.FROM_PAGE) }

    private var fetchingUserInfo = false

    //当前显示的tab是第几个
    val showTab = ObservableInt(0)

    //作品中是否是编辑状态
    val isEditing = ObservableBoolean(false)

    //设置编辑的回调
    var setIsEditing: ((Boolean) -> Unit)? = null


    fun initialization() {
        //获取用户主页信息接口
        fetchUserInfo()
    }

    override fun onResume() {
        super.onResume()
        isLogin.set(User.get().isLogged)
       /* if (isLogin.get()) {
            Global.refreshBalance()
        }*/
    }

    /**
     * 获取用户主页信息接口
     */
    fun fetchUserInfo() {
        if (User.get().isVisitor) {
            return
        }
        if (fetchingUserInfo) return
        fetchingUserInfo = true
        launch(
            block = {
                response(
                    bean = mRepo.api.getUserHomeInfo(UserTokenBody()),
                    result = {
                        fetchingUserInfo = false
                        User.get().updateFrom(this.data)
                        // IM 账号无效时先同步；自动登录 417/未登录时改走手动登录。
                        if (!ImSession.get().account.isValid()) {
                            syncImAccountAndLogin()
                        } else {
                            val imStatus = NIMClient.getStatus()
                            if (imStatus == StatusCode.UNLOGIN) {
                                AccountHelper.login()
                            }
                        }
                    },
                    error = { fetchingUserInfo = false }
                )
            },
            onError = { fetchingUserInfo = false }
        )
    }


    /** 查询并保存云信账号后登录；普通查询失败时继续走刷新 Token 的既有兜底链路。 */
    private fun syncImAccountAndLogin() {
        request(
            apiBlock = { mRepo.api.queryImAccount() },
            onError = { AccountHelper.login() },
            onSuccess = {
                val account = data
                User.get().storeImAccessId(account?.imAccId)
                User.get().storeImToken(account?.imAccToken)
                AccountHelper.login()
            }
        )
    }

    fun commonInit() {
        if (User.get().isVisitor) return
        launch {
            response(
                bean = mRepo.api.commonInit(),
                result = {})
        }
    }


    /**
     * 用户日活记录
     */
    fun daily() {
        if (User.get().isVisitor) return
        launch {
            response(
                bean = mRepo.api.daily(UserTokenBody()),
                result = {})
        }
    }

}