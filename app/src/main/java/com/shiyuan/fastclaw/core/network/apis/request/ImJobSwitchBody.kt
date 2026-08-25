package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 切换 IM 会话当前沟通职位请求体。
 */
data class ImJobSwitchBody(
    // 要切换到的岗位 ID。
    val currentJobId: Long,
)
