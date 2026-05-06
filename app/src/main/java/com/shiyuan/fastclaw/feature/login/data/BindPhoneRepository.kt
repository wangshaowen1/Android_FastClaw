package com.shiyuan.fastclaw.feature.login.data

import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.network.apis.request.BindPhoneBody
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 绑定手机号
 */
class BindPhoneRepository : AppRepository() {

    /**
     * 发送验证码
     */
    suspend fun sendSmsValidateCode(phoneNumber: String?) = withContext(Dispatchers.IO) {
        api.sendSmsValidateCode(
            SendSmsValidateCodeBody(
                phoneNumber,
                SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE
            )
        )
    }

    /**
     * 绑定手机
     */
    suspend fun bindPhone(phoneNumber: String?, validationCode: String?) =
        withContext(Dispatchers.IO) {
            api.bindPhone(BindPhoneBody(phoneNumber, validationCode))
        }
}