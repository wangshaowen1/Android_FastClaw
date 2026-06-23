package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 手机登录接口--闪验方式
 * @author xiaoowuu
 * @param flashValidateToken 闪验的token
 * @param inviteAttribution U-Link 邀请归因
 */
data class LoginByFlashMobileBody(
    val flashValidateToken: String?,
    val inviteAttribution: InviteAttribution? = null,
)
