package com.shiyuan.fastclaw.core.network.apis.response

import androidx.annotation.DrawableRes

data class AgentQuickData(
    @DrawableRes val iconRes: Int,
    val reportType: String,
)
