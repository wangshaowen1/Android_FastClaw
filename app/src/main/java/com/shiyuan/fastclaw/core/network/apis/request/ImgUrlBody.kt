package com.shiyuan.fastclaw.core.network.apis.request

data class ImgUrlBody(
    /**
     * 待校验的图片Url
     */
    val imgUrl: String?
) : UserTokenBody()