package com.kgmedia.fastfilm.core.network.apis.request

data class StoryboardIdBody(
    /**
     * 分镜ID
     */
    val storyboardId: String?
) : UserTokenBody()