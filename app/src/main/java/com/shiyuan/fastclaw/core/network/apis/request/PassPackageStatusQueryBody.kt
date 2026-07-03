package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询面试通关包生成状态请求体。
 *
 * @param packageId 通关包业务 ID
 */
data class PassPackageStatusQueryBody(
    val packageId: String? = null,
)

/**
 * 面试通关包重新生成请求体。
 *
 * @param packageId 通关包业务 ID；重新生成不允许更换简历或 JD
 */
data class PassPackageRegenerateBody(
    val packageId: String? = null,
)
