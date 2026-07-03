package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询面试通关包生成状态请求体。
 *
 * @param packageId 通关包业务 ID
 */
data class PassPackageStatusQueryBody(
    val packageId: String? = null,
)
