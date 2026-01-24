package com.kgmedia.fastfilm.feature.login.data

import com.fm.openinstall.OpenInstall
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.request.LoginByFlashMobileBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 闪验登录
 * @author xiaoowuu
 */
class FlashLoginRepository : ThirdLoginRepository() {
    /**
     * 闪验登录
     * @param flashValidateToken 闪验token
     */
    suspend fun loginByFlashMobile(flashValidateToken: String?) = withContext(Dispatchers.IO) {
        api.loginByFlashMobile(LoginByFlashMobileBody(flashValidateToken, Config.get().installData))
            .apply {
                val isNewRegister = data?.isNewRegistration() == true
                if (isNewRegister && Config.get().installFromOpenInstall()) {
                    OpenInstall.reportRegister()
                }
                EventManager.onAppTrack(
                    AppEvent.JG_ACTION_USER_LOGIN,
                    mapOf(GrowingIO.KEY_ALL_IS_NEW_USER to if (isNewRegister) "yes" else "no", GrowingIO.KEY_ALL_LOGIN_TYPE to "phone")
                )
            }
    }
}