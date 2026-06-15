package com.shiyuan.fastclaw.core.network.apis.request

/**
 * Banner 查询请求体
 *
 * @param bannerLocationCode Banner 展示位置编码
 * @see LOCATION_HOME 首页
 * @see LOCATION_PAY_POPUP 付费弹窗
 * @see LOCATION_PERSONAL_HOME 个人主页
 * @see LOCATION_MALL_PAGE 商城页面
 */
data class BannerBody(
    /**
     * 20260227启用新入参banner展示位置code（首页=banner_home 付费弹窗=banner_pay_popup，个人主页=banner_personal_home，商城页面=banner_mall_page
     */
    val bannerLocationCode: String?
) : UserTokenBody() {
    companion object {
        //消息页
        const val LOCATION_HOME = "banner_home"

        //付费弹窗
        const val LOCATION_PAY_POPUP = "banner_pay_popup"

        //个人主页
        const val LOCATION_PERSONAL_HOME = "banner_personal_home"

        //商城页面
        const val LOCATION_MALL_PAGE = "banner_mall_page"
    }
}