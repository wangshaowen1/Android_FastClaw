package com.kgmedia.fastfilm.router

import com.blankj.utilcode.util.AppUtils
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.feature.login.LoginDialog
import com.kgmedia.fastfilm.feature.login.flash.FlashLoginDialogFragment
import com.kgmedia.fastfilm.kit.SignUtils

object AppNavigator {

    /**
     * 强制退出并重启
     */
    @JvmStatic
    fun forceLogoutAndRestart() {
        if (User.get().isLogged) {
            SignUtils.signOut()
            AppUtils.relaunchApp()
        }
    }

    /**
     * 检查是否需要登录
     * 使用场景：点击某个按钮，如果没登录则跳转登录页，返回 true；如果已登录则返回 false，继续执行逻辑。
     * @return true: 已拦截(跳转去登录了) false: 未拦截(用户已登录)
     */
    @JvmStatic
    fun checkLoginIntercepted(showToast: Boolean = true): Boolean {
        if (User.get().isVisitor) {
            startLogin(showToast)
            return true
        }
        return false
    }

    fun startLogin(showToast: Boolean) {
        if (Constant.SDKConfiguration.IS_SHOW_WE_CHAT.not()) {
            FlashLoginDialogFragment.show(showToast, false)
            return
        }
        LoginDialog.show(showToast)
    }

}