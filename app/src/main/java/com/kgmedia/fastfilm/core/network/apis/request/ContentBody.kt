package com.kgmedia.fastfilm.core.network.apis.request

/**
 * @param content 待校验的文本内容
 */
data class ContentBody(
    /**
     * 待校验的文本内容
     */
    val content: String?
) : UserTokenBody()