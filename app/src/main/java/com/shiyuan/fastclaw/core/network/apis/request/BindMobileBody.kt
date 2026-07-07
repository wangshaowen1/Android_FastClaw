package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 三方登录后绑定手机号。
 *
 * @param telNum 待绑定手机号
 * @param smsValidateCode 手机短信验证码
 * @param bizType 短信业务类型，默认 2-换绑手机号
 * @param bindTicket 第三方首次登录待绑定手机号票据
 */
data class BindMobileBody(
    val telNum: String?,
    val smsValidateCode: String?,
    val bizType: Int = SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE,
    val bindTicket: String?
)
