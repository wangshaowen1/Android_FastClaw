package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 活动卡片 payload。
 */
data class ActivityPayloadBean(
    /** 活动类型，用于在 CARD_ACTIVITY 大类下继续细分具体卡片样式。 */
    val activityType: String? = null,
) {
    companion object {
        /** 测薪资活动。 */
        const val TYPE_SALARY = "SALARY"
    }
}
