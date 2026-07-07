package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 手机号绑定状态前置校验。
 *
 * @param mobile 待校验手机号
 * @param scene 校验场景，1-绑定手机号，2-换绑手机号
 * @param bindTicket 绑定手机号场景使用的绑定票据，来源于三方登录返回值 bindTicket
 */
data class CheckMobileBindStatusBody(
    val mobile: String?,
    val scene: Int,
    val bindTicket: String?
) {
    companion object {
        /** 绑定手机号 */
        const val SCENE_BIND_MOBILE = 1

        /** 换绑手机号 */
        const val SCENE_CHANGE_MOBILE = 2
    }
}
