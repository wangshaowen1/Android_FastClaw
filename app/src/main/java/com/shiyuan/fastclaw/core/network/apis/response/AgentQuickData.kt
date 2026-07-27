package com.shiyuan.fastclaw.core.network.apis.response

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class AgentQuickData(
    val type: AgentQuickType,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
)

enum class AgentQuickType {
    COMPANY_BACKGROUND,
    RESUME_CHECK,
    SALARY_CHECK,
    PASS_PACKAGE,
    MOCK_INTERVIEW,
}
