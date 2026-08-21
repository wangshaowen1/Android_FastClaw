package com.shiyuan.fastclaw.core.network.apis.response

/** 高重要度求职属性回答完成状态。 */
data class JobAgentAttributeAnswerCheckData(
    /** 当前启用的全部 HIGH 属性是否均已确认。 */
    val result: Boolean = false,
)
