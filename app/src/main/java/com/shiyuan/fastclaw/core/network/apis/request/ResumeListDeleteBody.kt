package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 删除我的简历列表项请求体
 *
 * @param resumeTaskId 简历解析任务业务 ID
 */
data class ResumeListDeleteBody(
    val resumeTaskId: Long,
)
