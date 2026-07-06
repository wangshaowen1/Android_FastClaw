package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 绑定第三方账号。
 *
 * @param providerType 第三方平台类型，1-微信，3-Apple
 * @param thirdPartyOpenId 第三方 openId
 * @param thirdPartyUnionId 第三方 unionId
 * @param thirdPartyNickName 第三方昵称
 * @param thirdPartyAvatar 第三方头像
 */
data class BindThirdPartyBody(
    val providerType: Int,
    val thirdPartyOpenId: String?,
    val thirdPartyUnionId: String?,
    val thirdPartyNickName: String?,
    val thirdPartyAvatar: String?,
    val authCode: String? = null,
    val identityToken: String? = null,
    val nonce: String? = null
) : UserTokenBody() {
    companion object {
        /** 微信 */
        const val PROVIDER_TYPE_WECHAT = 1

        /** Apple */
        const val PROVIDER_TYPE_APPLE = 3
    }
}
