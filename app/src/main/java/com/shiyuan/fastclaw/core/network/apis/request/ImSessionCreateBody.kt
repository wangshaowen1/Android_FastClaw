package com.shiyuan.fastclaw.core.network.apis.request

/** 创建 App 端 IM 会话请求体。 */
data class ImSessionCreateBody(
    // 当前沟通岗位 ID。
    val currentJobId: Long,
)
