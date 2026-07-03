package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询通关包简历状态请求体
 *
 * @param resumeBizId 指定简历业务 ID；为空时查询候选人最新可用简历
 */
data class InterviewPassPackageResumeStatusBody(
    val resumeBizId: Long? = null,
)
