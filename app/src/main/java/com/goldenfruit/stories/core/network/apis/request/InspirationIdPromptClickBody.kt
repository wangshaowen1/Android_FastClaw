package com.kgmedia.fastfilm.core.network.apis.request

data class InspirationIdPromptClickBody(
    val inspirationGroupId: String?,
    val inspirationId: String?,
    val usedSource: Int?,
):UserTokenBody()