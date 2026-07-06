package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 绑定第三方账号结果。
 *
 * @param status 绑定状态，0-绑定成功，1-第三方账号已被其他账号绑定，2-当前账号已绑定该第三方类型，3-旧账号无其他登录方式
 * @param message 前端展示提示文案
 * @param popupStyle 弹窗样式，1-透明弹窗，2-不透明弹窗
 */
data class BindThirdPartyData(
    val status: Int? = null,
    val message: String? = null,
    val popupStyle: Int? = null
) {
    fun isSuccess() = status == STATUS_SUCCESS

    companion object {
        /** 绑定成功 */
        const val STATUS_SUCCESS = 0

        /** 透明弹窗 */
        const val POPUP_STYLE_TRANSPARENT = 1

        /** 不透明弹窗 */
        const val POPUP_STYLE_OPAQUE = 2
    }
}
