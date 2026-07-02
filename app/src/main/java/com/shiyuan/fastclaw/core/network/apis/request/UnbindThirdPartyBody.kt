package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 解除第三方账号绑定。
 *
 * @param providerType 第三方平台类型，1-微信，3-Apple
 */
data class UnbindThirdPartyBody(
    val providerType: Int
) : UserTokenBody() {
    companion object {
        /** 微信 */
        const val PROVIDER_TYPE_WECHAT = 1
    }
}
