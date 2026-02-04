package com.kgmedia.fastfilm.feature.login.data

import com.fm.openinstall.OpenInstall
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.request.BindPhoneBody
import com.kgmedia.fastfilm.core.network.apis.request.LoginByPhoneBody
import com.kgmedia.fastfilm.core.network.apis.request.SendSmsValidateCodeBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 手机号登录
 */
class PhoneLoginRepository : AppRepository() {

    suspend fun sendSmsValidateCode(
        phoneNumber: String?,
        bizType: Int = SendSmsValidateCodeBody.BIZ_TYPE_LOGIN
    ) = withContext(Dispatchers.IO) {
        api.sendSmsValidateCode(
            SendSmsValidateCodeBody(
                phoneNumber,
                bizType = bizType
            )
        )
    }

    suspend fun loginByPhone(phoneNumber: String?, validationCode: String?) =
        withContext(Dispatchers.IO) {
            api.loginByPhone(
                LoginByPhoneBody(
                    telNum=phoneNumber,
                    smsValidateCode=validationCode,
                    bizType = LoginByPhoneBody.BIZ_TYPE_LOGIN,
                    Config.get().installData,
                )
            ).apply {
                val isNewRegister = data?.isNewRegistration() == true
                if (isNewRegister && Config.get().installFromOpenInstall()) {
                    OpenInstall.reportRegister()
                }
                EventManager.onAppTrack(
                    AppEvent.JG_ACTION_USER_LOGIN,
                    mapOf(
                        GrowingIO.KEY_ALL_IS_NEW_USER to if (isNewRegister) "yes" else "no",
                        GrowingIO.KEY_ALL_LOGIN_TYPE to "phone"
                    )
                )
            }
        }

    /**
     * 绑定手机
     */
    suspend fun bindPhone(phoneNumber: String?, validationCode: String?) =
        withContext(Dispatchers.IO) {
            api.bindPhone(BindPhoneBody(phoneNumber, validationCode))
        }
}