package com.kgmedia.fastfilm.core.network.apis.request

data class ImgUrlBody(
    /**
     * 待校验的图片Url
     */
    val imgUrl: String?
) : UserTokenBody()