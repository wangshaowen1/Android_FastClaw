package com.shiyuan.fastclaw.core.network.apis.request

/**
 * @desc：公司背调向服务端请求的数据
 * @author：CaoWei
 * @date：2026/6/22 10:16
 */

data class CompanyBackgroundStartBody(
    val companyName: String? = null,
    val companyId: Long? = null,
)

/**
 * 查询公司背调公司搜索结果
 *
 * @param companyName 用户输入的公司名称、简称、品牌名或缩写
 */
data class CompanyBackgroundCompanySearchQueryBody(
    val companyName: String?,
)

/**
 * 提交公司背调报告生成前置校验
 *
 * @param sessionId 小虾会话 ID
 * @param conversationId 小虾对话 ID
 * @param clientRequestId APP 本次点击的幂等 ID，同一次点击重试保持不变
 * @param companyId 公司搜索结果返回的公司 ID
 */
data class CompanyBackgroundPreCheckSubmitBody(
    /** 小虾会话 ID */
    val sessionId: String?,
    /** 小虾对话 ID */
    val conversationId: String?,
    /** APP 本次点击的幂等 ID，同一次点击重试保持不变 */
    val clientRequestId: String?,
    /** 公司搜索结果返回的公司 ID */
    val companyId: Long?,
)

/**
 * 查询公司背调报告生成前置校验结果
 *
 * @param preCheckId 前置校验任务 ID
 */
data class CompanyBackgroundPreCheckResultQueryBody(
    val preCheckId: String?,
)

/**
 * 查询公司背调报告生成结果
 *
 * @param reportId 报告 ID
 */
data class CompanyBackgroundGenerateResultQueryBody(
    val reportId: String?,
)

/**
 * 重新生成公司背调失败报告
 *
 * @param failedReportId 原生成失败报告 ID
 * @param clientRequestId APP 本次点击的幂等 ID，同一次点击重试保持不变
 */
data class CompanyBackgroundRegenerateSubmitBody(
    val failedReportId: String?,
    val clientRequestId: String?,
)
