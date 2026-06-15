package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 内容查询请求体
 *
 * @param content 内容
 */
data class ContentBody(
    val content: String? = null,
) : UserTokenBody()