package com.shiyuan.fastclaw.core.network.apis.entity

/**
 * 云信 IM Token 刷新结果。
 * @param imReady 云信账号是否就绪
 * @param imAccId 云信账号 ID
 * @param imAccToken 云信登录 token
 */
data class ImTokenData(
    var imReady: Boolean? = null,
    var imAccId: String? = null,
    var imAccToken: String? = null
)
