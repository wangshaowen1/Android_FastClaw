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

    // 底部机会 Tab 展示的有效未处理机会数。
    val opportunityUnprocessedCount = MutableLiveData(0)

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
            queryOpportunityNavCount()
        } else {
            clearImUnreadCount()
            clearOpportunityNavCount()
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
                        User.get().updateFrom(this.data)
                        syncImAccountAndLogin()
                    },
                    error = { fetchingUserInfo = false }
                )
            },
            onError = { fetchingUserInfo = false }
        )
    }


    /**
     * 查询并保存云信账号后登录，用户信息刷新成功后必须同步最新 IM 账号。
     */
    private fun syncImAccountAndLogin() {
        request(
            apiBlock = { mRepo.api.queryImAccount() },
            onError = {
                Logger.i(
                    "ImLogin",
                    "syncImAccountAndLogin localValid=${ImSession.get().account}, " +
                        "logged=${AccountHelper.isLogged()}"
                )
                AccountHelper.login()
                refreshImData()
            },
            onSuccess = {
                Logger.i("ImLogin",
                    "fetchUserInfo imAccId=${this.data?.imAccId}, " +
                            "localValid=${ImSession.get().account}, " +
                            "logged=${AccountHelper.isLogged()}"
                )
                /*data?.imAccId = "234567"
                data?.imAccToken = "234567"
                data?.ready = true*/

                User.get().storeImAccessId(data?.imAccId)
                User.get().storeImToken(data?.imAccToken)
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
     * 查询底部机会 Tab 的有效未处理机会数，游客状态下不发起请求。
     */
    fun queryOpportunityNavCount() {
        if (User.get().isVisitor) {
            opportunityUnprocessedCount.value = 0
            return
        }
        request(
            apiBlock = { mRepo.api.queryOpportunityNavCount() },
            onSuccess = {
                data?.let { opportunityUnprocessedCount.value = it.unprocessedCount }
            }
            /*, onError = {
                opportunityUnprocessedCount.value = 10
            }*/
        )
    }

    /** 清空底部机会 Tab 的未处理数量。 */
    fun clearOpportunityNavCount() {
        opportunityUnprocessedCount.value = 0
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
