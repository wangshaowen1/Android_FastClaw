package com.shiyuan.fastclaw.router

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.common.Constant
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.feature.login.flash.FlashLoginDialogFragment
import com.shiyuan.fastclaw.feature.login.flash.PhoneLoginDialogActivity
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginActivity
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginDialog
import com.shiyuan.fastclaw.feature.login.phone.PhoneNewLoginActivity
import com.shiyuan.fastclaw.feature.login.unified.UnifiedLoginActivity
import com.shiyuan.fastclaw.feature.login.unified.UnifiedLoginDialog
import com.shiyuan.fastclaw.kit.SignUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.text.get

object AppNavigator {

    private val isHandlingUserNotLoggedIn = AtomicBoolean(false)
    /**
     * 统一处理登录失效：并发触发只响应一次，提示指定文案并清空页面栈返回登录页。
     *
     * @param message 退出登录前展示的提示文案
     * @param delayToLoginMillis 提示后延迟返回登录页的时长，0 表示立即返回
     */
    @JvmStatic
    fun forceUserNotLoggedIn(message: String?, delayToLoginMillis: Long = 0L) {
        if (User.get().isVisitor || !isHandlingUserNotLoggedIn.compareAndSet(false, true)) return
        App.instance.post(
            Runnable {
                val returnToLogin = Runnable {
                    SignUtils.signOut()
                    App.instance.startActivity(
                        Intent(App.instance, PhoneNewLoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    isHandlingUserNotLoggedIn.set(false)
                }
                if (delayToLoginMillis > 0 && !message.isNullOrBlank()) {
                    val toast = Toast.makeText(App.instance, message, Toast.LENGTH_LONG)
                    toast.show()
                    App.instance.postDelay(
                        Runnable {
                            toast.cancel()
                            returnToLogin.run()
                        },
                        delayToLoginMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    )
                } else {
                    message?.takeIf { it.isNotBlank() }?.showToast()
                    returnToLogin.run()
                }
            }
        )
    }


    /**
     * 强制退出并打开登录
     */
    @JvmStatic
    fun forceLogoutAndLogin(loginSource: String? = null) {
        if (!User.get().isVisitor) {
            SignUtils.signOut()
            startLogin(showToast = false, loginSource = loginSource)
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
        isFullScreen: Boolean = false,
        loginSource: String? = null
    ): Boolean {
        if (User.get().isVisitor) {
            startLogin(showToast, isFullScreen, loginSource)
            return true
        }
        return false
    }

    fun startLogin(
        showToast: Boolean,
        isFullScreen: Boolean = false,
        loginSource: String? = null
    ) {
        FlashLoginDialogFragment.show(
            showToast = showToast,
            checkedPolicy = false,
            loginSource = loginSource
        )

        //PhoneLoginActivity.start(showToast = showToast, checkedPolicy = false)
/*        if (Constant.SDKConfiguration.IS_SHOW_WE_CHAT.not()) {
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
        }*/
    }
}
