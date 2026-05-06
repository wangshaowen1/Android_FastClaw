package com.shiyuan.fastclaw.router

import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.AppUtils
import com.shiyuan.fastclaw.common.Constant
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.feature.login.flash.PhoneLoginDialogActivity
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginActivity
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginDialog
import com.shiyuan.fastclaw.feature.login.unified.UnifiedLoginActivity
import com.shiyuan.fastclaw.feature.login.unified.UnifiedLoginDialog
import com.shiyuan.fastclaw.kit.SignUtils

object AppNavigator {

    /**
     * 强制退出并重启
     */
    @JvmStatic
    fun forceLogoutAndRestart() {
        if (!User.get().isVisitor) {
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
    fun checkLoginIntercepted(
        showToast: Boolean = true,
        isFullScreen: Boolean = false
    ): Boolean {
        if (User.get().isVisitor) {
            startLogin(showToast, isFullScreen)
            return true
        }
        return false
    }

    fun startLogin(
        showToast: Boolean,
        isFullScreen: Boolean = false
    ) {
        if (Constant.SDKConfiguration.IS_SHOW_WE_CHAT.not()) {
            if (isFullScreen) {
                PhoneLoginActivity.start(showToast = showToast, checkedPolicy = false)
            } else if (topActivity != null && topActivity is AppCompatActivity) {
                (topActivity as AppCompatActivity).apply {
                    PhoneLoginDialog.show(
                        showToast = showToast,
                        checkedPolicy = false,
                        manager = this.supportFragmentManager
                    )
                }
            }
            return
        }
        if (isFullScreen) {
            UnifiedLoginActivity.start(showToast = showToast)
        } else if (topActivity != null && topActivity is AppCompatActivity) {
            (topActivity as AppCompatActivity).apply {
                UnifiedLoginDialog.show(
                    showToast = showToast,
                    manager = this.supportFragmentManager
                )
            }
        }
    }
}