package com.kgmedia.fastfilm.core.network.apis.request

/**
 * {
 *     "ids": [
 *         "泛型Long"
 *     ],
 *     "id": "id"
 * }
 */
data class IdsBody(
    val ids: List<String>?,
    val id: String?
)