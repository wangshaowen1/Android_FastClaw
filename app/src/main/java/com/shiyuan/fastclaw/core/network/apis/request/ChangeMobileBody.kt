package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 换绑手机号。
 *
 * @param newTelNum 新手机号
 * @param smsValidateCode 新手机号短信验证码
 * @param bizType 短信业务类型，默认 2-换绑手机号
 */
data class ChangeMobileBody(
    val newTelNum: String?,
    val smsValidateCode: String?,
    val bizType: Int = SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE
) : UserTokenBody()
