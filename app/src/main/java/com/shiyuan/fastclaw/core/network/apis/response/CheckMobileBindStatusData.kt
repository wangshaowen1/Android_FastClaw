package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 手机号绑定状态前置校验结果。
 *
 * @param status 校验状态，0-可用，1-不可用
 * @param message 前端展示提示文案
 * @param popupStyle 弹窗样式，1-透明弹窗，2-不透明弹窗
 */
data class CheckMobileBindStatusData(
    val status: Int? = null,//校验状态，0-可用，1-不可用
    val message: String? = null,//前端展示提示文案
    val popupStyle: Int? = null//弹窗样式，1-透明弹窗，2-不透明弹窗
) {
    fun isSuccess() = status == STATUS_AVAILABLE

    companion object {
        const val STATUS_AVAILABLE = 0
    }
}
