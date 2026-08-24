package com.shiyuan.fastclaw.core.network.apis.request

/** 创建企业侧 IM 会话请求体。 */
data class ImSessionCreateBody(
    val targetSubjectType: Int,
    val targetBaseAccountId: Long,
    val currentJobId: Long,
)
