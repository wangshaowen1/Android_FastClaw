package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
{
"workId": "作品 ID",
"userId": "作者用户 ID"
}
 * }
 */
data class PlayEventBody(
    val userId: String? = null,
    val workId: String? = null
) : UserTokenBody()