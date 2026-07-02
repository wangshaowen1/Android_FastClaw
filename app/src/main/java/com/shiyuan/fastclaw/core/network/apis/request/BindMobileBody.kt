package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 绑定手机号。
 *
 * @param telNum 待绑定手机号
 * @param smsValidateCode 手机短信验证码
 * @param bizType 短信业务类型，默认 2-换绑手机号
 * @param userToken 三方登录返回的临时登录 token
 */
class BindMobileBody(
    val telNum: String?,
    val smsValidateCode: String?,
    val bizType: Int = SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE,
    userToken: String?
) : UserTokenBody(userToken)
