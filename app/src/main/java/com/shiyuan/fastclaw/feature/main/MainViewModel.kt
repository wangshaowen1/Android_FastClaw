package com.shiyuan.fastclaw.feature.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.netease.nimlib.sdk.NIMClient
import com.netease.nimlib.sdk.StatusCode
import com.netease.nimlib.sdk.msg.model.IMMessage
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Global
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.response
import com.nhsd.basic.utils.Logger
import com.tulele.im.ImSession
import com.tulele.im.StateChangeListener
import com.tulele.im.conversation.ConversationListener
import com.tulele.im.conversation.IConversationManager
import com.tulele.im.entity.Conversation
import com.tulele.im.utils.AccountHelper
import kotlin.text.get
import com.shiyuan.fastclaw.core.ext.request

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
class MainViewModel : AppRepositoryViewModel<AppRepository>(), ConversationListener {

    //显示隐藏底部
    val showBottomBar = ObservableBoolean(true)

    //是否登录
    val isLogin by lazy { ObservableBoolean(false) }
    val fromPage by lazy { mBundle.getString(Constants.Extra.FROM_PAGE) }

    private var fetchingUserInfo = false

    //当前显示的tab是第几个
    val showTab = ObservableInt(0)

    val imUnreadCount = MutableLiveData(0)

    //作品中是否是编辑状态
    val isEditing = ObservableBoolean(false)

    //设置编辑的回调
    var setIsEditing: ((Boolean) -> Unit)? = null

    private val conversationManager get() = ImSession.get().getConversationManager()
    private var registeredConversationManager: IConversationManager? = null
    private var imListenersRegistered = false

    private val imStateChangeListener = StateChangeListener { statusCode ->
        if (statusCode == StatusCode.LOGINED) {
            refreshImData()
        }
    }

    private val onMessageReceiveListener = object : ImSession.OnMessageReceiveListener {
        /**
         * 收到 IM 新消息后刷新底部消息 Tab 未读数。
         */
        override fun onMessageReceived(imMessage: List<IMMessage>?) {
            refreshImUnreadCount()
        }
    }

    /**
     * 初始化主界面基础数据，并开始监听 IM 会话未读数。
     */
    fun initialization() {
        //获取用户主页信息接口
        fetchUserInfo()
        registerImListeners()
        refreshImUnreadCount()
    }

    /**
     * 页面恢复时补拉 IM 未读数，覆盖后台期间错过的会话变化。
     */
    override fun onResume() {
        super.onResume()
        isLogin.set(User.get().isLogged)
        if (User.get().isLogged) {
            registerImListeners()
            refreshImUnreadCount()
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
                        User.get().updateFrom(this.data)
                        Logger.i(
                            IM_LOGIN_LOG_TAG,
                            "fetchUserInfo imReady=${this.data?.imReady}, " +
                                "imAccId=${this.data?.imAccId}, " +
                                "imAccToken=${this.data?.imAccToken}, " +
                                "localValid=${ImSession.get().account.isValid}, " +
                                "nimStatus=${NIMClient.getStatus()}"
                        )
                        // IM 账号无效时先同步；自动登录 417/未登录时改走手动登录。
                        if (!ImSession.get().account.isValid()) {
                            syncImAccountAndLogin()
                        } else {
                            val imStatus = NIMClient.getStatus()
                            if (imStatus == StatusCode.UNLOGIN) {
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
                    "syncImAccountAndLogin localValid=${ImSession.get().account.isValid}, " + "nimStatus=${NIMClient.getStatus()}"
                )
                AccountHelper.login()
                refreshImData()
            },
            onSuccess = {
                val account = data
                Logger.i(IM_LOGIN_LOG_TAG,
                    "syncImAccountAndLogin imReady=${account?.imReady}, " +
                        "imAccId=${account?.imAccId}, " +
                        "imAccToken=${account?.imAccToken}, " +
                        "nimStatus=${NIMClient.getStatus()}"
                )
                User.get().storeImAccessId(account?.imAccId)
                User.get().storeImToken(account?.imAccToken)
                AccountHelper.login()
                refreshImData()
            }
        )
    }

    /**
     * 登录态可用时同步 IM 数据，并刷新底部消息 Tab 未读数。
     */
    fun refreshImData() {
        registerImListeners()
        refreshImUnreadCount()
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
     * 注册 IM 消息、登录状态和会话监听，避免重复添加全局监听。
     */
    private fun registerImListeners() {
        registerImConversationListener()
        if (imListenersRegistered) return
        ImSession.get().apply {
            addOnMessageReceiveListener(onMessageReceiveListener)
            addStateChangeListeners(imStateChangeListener)
        }
        imListenersRegistered = true
    }

    /**
     * 移除 IM 消息、登录状态和会话监听，避免 ViewModel 销毁后继续收到回调。
     */
    private fun unregisterImListeners() {
        removeImConversationListener()
        if (!imListenersRegistered) return
        ImSession.get().apply {
            removeOnMessageReceiveListener(onMessageReceiveListener)
            removeStateChangeListeners(imStateChangeListener)
        }
        imListenersRegistered = false
    }

    /**
     * 绑定 IM 会话监听，重复进入主界面时复用已绑定的会话管理器。
     */
    private fun registerImConversationListener() {
        val manager = conversationManager
        if (registeredConversationManager === manager) return
        registeredConversationManager?.removeConversationListener(this)
        manager.addConversationListener(this)
        registeredConversationManager = manager
    }

    /**
     * 移除 IM 会话监听，避免主 ViewModel 销毁后继续收到未读数回调。
     */
    private fun removeImConversationListener() {
        registeredConversationManager?.removeConversationListener(this)
        registeredConversationManager = null
    }

    /**
     * 查询 IM 会话未读总数并回填主 Tab 红点数据。
     */
    private fun refreshImUnreadCount() {
        imUnreadCount.value = queryImUnreadCount()
    }

    /**
     * 查询 IM SDK 会话未读总数，未登录或 SDK 查询失败时返回 0。
     */
    private fun queryImUnreadCount(): Int {
        if (!AccountHelper.isLogged()) return 0
        return try {
            conversationManager.getTotalUnreadCount()
        } catch (_: Exception) {
            0
        }
    }

    /**
     * IM 首次加载会话列表后刷新主 Tab 未读数。
     */
    override fun onConversationsLoaded(conversations: List<Conversation>?) {
        refreshImUnreadCount()
    }

    /**
     * IM 会话新增或更新时刷新主 Tab 未读数。
     */
    override fun onConversationsChanged(conversations: List<Conversation>) {
        refreshImUnreadCount()
    }

    /**
     * IM 会话删除后刷新主 Tab 未读数。
     */
    override fun onConversationDeleted(conversation: Conversation) {
        refreshImUnreadCount()
    }

    /**
     * IM 会话状态变化时刷新主 Tab 未读数。
     */
    override fun onConversationStateChanged(conversation: Conversation) {
        refreshImUnreadCount()
    }

    /**
     * ViewModel 销毁时移除 IM 消息、登录状态和会话监听。
     */
    override fun onDestroy() {
        unregisterImListeners()
        super.onDestroy()
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
