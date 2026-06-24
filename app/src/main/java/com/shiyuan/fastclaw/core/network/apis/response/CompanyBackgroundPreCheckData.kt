package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 公司背调报告生成前置校验提交结果
 *
 * @param preCheckId 前置校验任务 ID
 */
data class CompanyBackgroundPreCheckSubmitData(
    val preCheckId: String? = null,
)

/**
 * 公司背调报告生成前置校验轮询结果
 *
 * @param finished 前置校验是否结束
 * @param passed 前置校验是否通过，finished=false 时固定为 false
 * @param errorCode 校验失败编码：POINTS_INSUFFICIENT / NOT_COMPANY / MODEL_FAILED
 * @param errorMessage 校验失败时给 APP 展示的错误提示
 * @param reportId 前置校验通过后返回的报告 ID
 */
data class CompanyBackgroundPreCheckResultData(
    val finished: Boolean? = null,
    val passed: Boolean? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val reportId: String? = null,
)

/**
 * 公司背调报告生成轮询结果
 *
 * @param finished 报告是否已到终态，成功和失败都返回 true
 */
data class CompanyBackgroundGenerateResultData(
    val finished: Boolean? = null,
)

/**
 * 重新生成公司背调失败报告提交结果
 *
 * @param reportId 新报告 ID，用于后续生成结果轮询
 */
data class CompanyBackgroundRegenerateSubmitData(
    val reportId: String? = null,
)
