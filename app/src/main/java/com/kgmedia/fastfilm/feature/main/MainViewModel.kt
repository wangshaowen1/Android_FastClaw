package com.kgmedia.fastfilm.feature.main

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.GuideBarStatusInfo
import com.kgmedia.fastfilm.core.network.apis.request.AppFloatWindowsBody
import com.kgmedia.fastfilm.core.network.apis.request.DramasIdBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.network.apis.response.AppFloatWindowsInfo
import com.kgmedia.fastfilm.core.network.apis.response.DramasInfoData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.play.more.ShareShortDramaViewModel
import com.kgmedia.fastfilm.router.AppRouter

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/4/17  9:38
 * @Version: 1.0
 * *****************************************
 */
class MainViewModel : ShareShortDramaViewModel<AppRepository>() {

    //显示隐藏底部
    val showBottomBar = ObservableBoolean(true)

    //是否登录
    val isLogin by lazy { ObservableBoolean(false) }

    private var fetchingUserInfo = false

    //当前显示的tab是第几个
    val showTab = ObservableInt(1)

    //去创作
    val startCreating = MutableLiveData<Boolean>()

    //悬浮球信息
    val appFloatWindows = ObservableField<AppFloatWindowsInfo?>()

    //val generating get() = GeneratingViewModel
    val generating = ObservableField<DramasInfoData>()
    val unreadNum by lazy { ObservableInt() }

    //第一个作品引导条
    val firstWorkGuideBar = ObservableField<GuideBarStatusInfo>()

    //是否强制关闭第一个作品引导条
    var forceCloseFirstWorkGuideBar = false

    fun initialization() {
        //获取用户主页信息接口
        fetchUserInfo()
        //获取用户主页信息接口
        //getAppFloatWindowsInfo()
    }

    override fun onResume() {
        super.onResume()
        isLogin.set(User.get().isLogged)
        if (isLogin.get()) {
            messagesUnreadNum()
        } else {
            unreadNum.set(0)
        }
        if (User.get().isLogged) {
            getFirstWorkGuideBarInfo()
            getGeneratingInfo()
        }
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
                    },
                    error = { fetchingUserInfo = false }
                )
            },
            onError = { fetchingUserInfo = false }
        )
    }

    fun getAppFloatWindowsInfo() {
        launch {
            response(
                bean = mRepo.api.getAppFloatWindows(AppFloatWindowsBody()),
                result = {
                    appFloatWindows.set(data?.items?.firstOrNull { windowsInfo -> windowsInfo.floatIconCode == AppFloatWindowsInfo.FLOAT_ICON_CODE_HOME })
                }
            )
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

    /**
     * 获取消息未读数量
     */
    fun messagesUnreadNum() {
        launch {
            response(mRepo.api.messagesUnreadNum(UserTokenBody())) {
                unreadNum.set(data?.unreadNumTotal ?: 0)
            }
        }
    }

    fun getTabColorSelector(showTab: Int): Int {
        return if (showTab == 0) R.color.main_tab_txt_color_dark else R.color.main_tab_txt_color
    }

    private fun getFirstWorkGuideBarInfo() {
        if (forceCloseFirstWorkGuideBar) {
            firstWorkGuideBar.set(null)
            return
        }
        launch {
            response(
                bean = mRepo.api.newUserCreationGuideBar(UserTokenBody()),
                result = {
                    firstWorkGuideBar.set(if (forceCloseFirstWorkGuideBar) null else data)
                })
        }
    }

    fun clickFirstWorkGuideBar() {
        firstWorkGuideBar.get()?.dramasId?.let {
            AppRouter.startShortDrama(it)
        }
    }

    fun forceCloseFirstWorkGuideBar() {
        forceCloseFirstWorkGuideBar = true
        firstWorkGuideBar.set(null)
    }

    private fun getGeneratingInfo() {
        launch {
            response(
                bean = mRepo.api.getGeneratingInfo(UserTokenBody()),
                result = {
                    generating.set(data)
                })
        }
    }

    fun closeGeneratingDramas() {
        val dramasId = generating.get()?.dramasId
        generating.set(null)
        launch {
            response(
                mRepo.api.closeGeneratingDramas(DramasIdBody(dramasId)),
            ) {}
        }
    }
}