package com.kgmedia.fastfilm.core.network.apis.request

data class LyricPromptBody(
    val lyricPrompt: String?,
    val musicGenConfigId: String?
) : UserTokenBody()