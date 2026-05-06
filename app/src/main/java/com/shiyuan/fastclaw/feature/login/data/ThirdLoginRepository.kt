package com.shiyuan.fastclaw.feature.login.data

import com.fm.openinstall.OpenInstall
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.manager.GrowingIO
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 第三方登录
 */
open class ThirdLoginRepository : AppRepository() {
    /**
     * 第三方登录
     */
    suspend fun loginByThird(body: LoginByThirdBody) = withContext(Dispatchers.IO) {
        api.loginByThird(body).apply {
            val isNewRegister = data?.isNewRegistration() == true
            if (isNewRegister && Config.get().installFromOpenInstall()) {
                OpenInstall.reportRegister()
            }

//            EventManager.onAppTrack(
//                AppEvent.JG_ACTION_USER_LOGIN,
//                mapOf(GrowingIO.KEY_ALL_IS_NEW_USER to if (isNewRegister) "yes" else "no", GrowingIO.KEY_ALL_LOGIN_TYPE to "wechat")
//            )
        }
    }
}