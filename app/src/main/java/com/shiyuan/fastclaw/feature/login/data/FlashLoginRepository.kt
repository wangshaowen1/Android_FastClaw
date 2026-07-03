package com.shiyuan.fastclaw.feature.login.data

import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.manager.GrowingIO
import com.shiyuan.fastclaw.core.network.apis.request.BindMobileBody
import com.shiyuan.fastclaw.core.network.apis.request.BindPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.ChangeMobileBody
import com.shiyuan.fastclaw.core.network.apis.request.CheckMobileBindStatusBody
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
        api.loginByFlashMobile(
            LoginByFlashMobileBody(
                flashValidateToken = flashValidateToken,
                inviteAttribution = Config.get().inviteAttribution,
            )
        )
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

    /**
     * 手机号绑定状态前置校验。
     */
    suspend fun checkMobileBindStatus(phoneNumber: String?, scene: Int, userToken: String?) =
        withContext(Dispatchers.IO) {
            api.checkMobileBindStatus(CheckMobileBindStatusBody(phoneNumber, scene, userToken))
        }

    suspend fun loginByPhone(phoneNumber: String?, validationCode: String?) =
        withContext(Dispatchers.IO) {
            api.loginByPhone(
                LoginByPhoneBody(
                    telNum=phoneNumber,
                    smsValidateCode=validationCode,
                    bizType = LoginByPhoneBody.BIZ_TYPE_LOGIN,
                    inviteAttribution = Config.get().inviteAttribution,
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

    /**
     * 三方登录后绑定手机。
     */
    suspend fun bindMobile(phoneNumber: String?, validationCode: String?, userToken: String?) =
        withContext(Dispatchers.IO) {
            api.bindMobile(BindMobileBody(phoneNumber, validationCode, userToken = userToken))
        }

    /**
     * 换绑手机号
     */
    suspend fun changeMobile(phoneNumber: String?, validationCode: String?) =
        withContext(Dispatchers.IO) {
            api.changeMobile(ChangeMobileBody(phoneNumber, validationCode))
        }
}
