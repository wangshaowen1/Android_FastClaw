package com.kgmedia.fastfilm.core.network.apis.request

data class BannerBody(
    /**
     * 展示位置code（首页=banner_home）
     */
    val code: String?
) : UserTokenBody() {
    companion object {
        //消息页
        const val LOCATION_HOME = "banner_home"
    }
}