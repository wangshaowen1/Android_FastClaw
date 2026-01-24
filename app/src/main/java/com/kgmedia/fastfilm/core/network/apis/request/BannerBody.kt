package com.kgmedia.fastfilm.core.network.apis.request

data class BannerBody(
    /**
     * banner位置 1-消息页
     */
    val bannerLocation: Int?
){
    companion object{
        //消息页
        const val LOCATION_MESSAGE = 1
    }
}