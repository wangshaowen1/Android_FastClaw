package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 图片 URL 校验请求体
 *
 * @param imgUrl 待校验的图片 URL
 */
data class ImgUrlBody(
    /** 待校验的图片 URL */
    val imgUrl: String?
) : UserTokenBody()