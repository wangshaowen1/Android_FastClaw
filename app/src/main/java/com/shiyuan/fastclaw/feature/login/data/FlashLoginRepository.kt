package com.shiyuan.fastclaw.feature.login.data

import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.manager.GrowingIO
import com.shiyuan.fastclaw.core.network.apis.request.BindPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByFlashMobileBody
import com.shiyuan.fastclaw.core.network.apis.request.LoginByPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
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
//                EventManager.onAppTrack(
//                    AppEvent.JG_ACTION_USER_LOGIN,
//                    mapOf(GrowingIO.KEY_ALL_IS_NEW_USER to if (isNewRegister) "yes" else "no", GrowingIO.KEY_ALL_LOGIN_TYPE to "phone")
//                )
            }
    }

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
//                EventManager.onAppTrack(
//                    AppEvent.JG_ACTION_USER_LOGIN,
//                    mapOf(
//                        GrowingIO.KEY_ALL_IS_NEW_USER to if (isNewRegister) "yes" else "no",
//                        GrowingIO.KEY_ALL_LOGIN_TYPE to "phone"
//                    )
//                )
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
