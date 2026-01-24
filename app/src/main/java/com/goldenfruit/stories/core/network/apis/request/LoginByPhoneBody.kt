package com.kgmedia.fastfilm.core.network.apis.request

/**
 * 手机登录接口--验证码方式
 * @author xiaoowuu
 * @param telNum 手机号码
 * @param smsValidateCode 验证码
 * @param inviteData 邀请数据--openinstall参数
 */
data class LoginByPhoneBody(val telNum: String?, val smsValidateCode: String?, val inviteData: String?)