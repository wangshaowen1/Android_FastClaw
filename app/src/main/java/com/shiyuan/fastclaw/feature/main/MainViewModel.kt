package com.shiyuan.fastclaw.feature.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.response
import com.nhsd.basic.utils.Logger
import com.tulele.im.ImAuthState
import com.tulele.im.ImSession
import com.tulele.im.StateChangeListener
import com.tulele.im.utils.AccountHelper
import kotlinx.coroutines.Job

private const val IM_LOGIN_LOG_TAG = "ImLogin"

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

    // 底部消息 Tab 展示的 IM 总未读数。
    val imUnreadCount = MutableLiveData(0)

    //作品中是否是编辑状态
    val isEditing = ObservableBoolean(false)

    //设置编辑的回调
    var setIsEditing: ((Boolean) -> Unit)? = null

    // 当前主 ViewModel 是否已经注册 IM 登录状态监听。
    private var imListenersRegistered = false
    // 未读数持续订阅任务，避免重复收集同一状态流。
    private var unreadCountCollectionJob: Job? = null

    private val imStateChangeListener = StateChangeListener { statusCode ->
        if (statusCode == ImAuthState.LOGGED_IN) {
            refreshImData()
        }
    }

    /**
     * 初始化主界面基础数据，并开始监听 IM 会话未读数。
     */
    fun initialization() {
        //获取用户主页信息接口
        fetchUserInfo()
        registerImListeners()
    }

    /**
     * 页面恢复时确保 IM 登录状态监听和未读数订阅已启动。
     */
    override fun onResume() {
        super.onResume()
        isLogin.set(User.get().isLogged)
        if (User.get().isLogged) {
            registerImListeners()
        } else {
            clearImUnreadCount()
        }
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

                        /*data?.imAccId = "234567"
                        data?.imAccToken = "234567"
                        data?.imReady = true*/

                        User.get().updateFrom(this.data)
                        Logger.i(
                            IM_LOGIN_LOG_TAG,
                            "fetchUserInfo imReady=${this.data?.imReady}, " +
                                "localValid=${ImSession.get().account.isValid}, " +
                                "logged=${AccountHelper.isLogged()}"
                        )
                        // IM 账号无效时先同步；已有账号但组件未登录时走组件登录入口。
                        if (!ImSession.get().account.isValid()) {
                            syncImAccountAndLogin()
                        } else {
                            if (!AccountHelper.isLogged()) {
                                AccountHelper.login()
                            }
                            refreshImData()
                        }
                    },
                    error = { fetchingUserInfo = false }
                )
            },
            onError = { fetchingUserInfo = false }
        )
    }


    /**
     * 查询并保存云信账号后登录，普通查询失败时继续走刷新 Token 的既有兜底链路。
     */
    private fun syncImAccountAndLogin() {
        request(
            apiBlock = { mRepo.api.queryImAccount() },
            onError = {
                Logger.i(
                    IM_LOGIN_LOG_TAG,
                    "syncImAccountAndLogin localValid=${ImSession.get().account.isValid}, " +
                        "logged=${AccountHelper.isLogged()}"
                )
                AccountHelper.login()
                refreshImData()
            },
            onSuccess = {
                val account = data
                Logger.i(IM_LOGIN_LOG_TAG,
                    "syncImAccountAndLogin imReady=${account?.imReady}, " +
                        "hasAccount=${!account?.imAccId.isNullOrBlank()}, " +
                        "hasToken=${!account?.imAccToken.isNullOrBlank()}"
                )
                User.get().storeImAccessId(account?.imAccId)
                User.get().storeImToken(account?.imAccToken)
                AccountHelper.login()
                refreshImData()
            }
        )
    }

    /**
     * 登录态可用时同步 IM 数据，未读数由统一状态流持续推送。
     */
    fun refreshImData() {
        registerImListeners()
        if (AccountHelper.isLogged()) {
            ImSession.get().syncIMData()
        }
    }

    /**
     * 清空底部消息 Tab 未读数，避免退出登录后保留上一账号红点。
     */
    fun clearImUnreadCount() {
        imUnreadCount.value = 0
    }

    /**
     * 注册 IM 登录状态监听并启动未读数订阅，避免重复收集和添加监听。
     */
    private fun registerImListeners() {
        startUnreadCountCollection()
        if (imListenersRegistered) return
        ImSession.get().addStateChangeListeners(imStateChangeListener)
        imListenersRegistered = true
    }

    /**
     * 停止未读数订阅并移除 IM 登录状态监听。
     */
    private fun unregisterImListeners() {
        unreadCountCollectionJob?.cancel()
        unreadCountCollectionJob = null
        if (!imListenersRegistered) return
        ImSession.get().removeStateChangeListeners(imStateChangeListener)
        imListenersRegistered = false
    }

    /**
     * 持续收集组件聚合后的未读数，由 ViewModel 生命周期统一取消。
     */
    private fun startUnreadCountCollection() {
        if (unreadCountCollectionJob?.isActive == true) return
        unreadCountCollectionJob = launch {
            ImSession.get().unreadCounter.collect { count ->
                imUnreadCount.value = count
            }
        }
    }

    /**
     * ViewModel 销毁时停止未读数订阅并移除 IM 登录状态监听。
     */
    override fun onDestroy() {
        unregisterImListeners()
        super.onDestroy()
    }

    /**
     * 拉取公共初始化数据，游客状态下跳过接口请求。
     */
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
