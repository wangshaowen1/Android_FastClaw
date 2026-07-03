package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 面试通关包生成状态。
 */
data class PassPackageStatusData(
    /** 通关包业务 ID */
    val packageId: String? = null,
    /** 生成批次号 */
    val generateBatchNo: Int? = null,
    /** 通关包状态：0 初始化，1 生成中，2 生成成功，3 生成失败，4 异常阻断 */
    val packageStatus: Int? = null,
    /** 生成进度，0-100 */
    val progress: Int? = null,
    /** 是否允许重新生成 */
    val canRegenerate: Boolean? = null,
    /** 失败原因 */
    val failReason: String? = null,
    /** 对用户可见时间，格式 yyyy-MM-dd HH:mm:ss */
    val visibleTime: String? = null,
    /** 通关包模块生成状态 */
    val modules: List<PassPackageModuleStatusData>? = null,
)

/**
 * 面试通关包重新生成结果。
 */
data class PassPackageRegenerateData(
    /** 通关包业务 ID */
    val packageId: String? = null,
    /** 新生成批次号 */
    val generateBatchNo: Int? = null,
    /** 通关包状态，重新生成成功后为生成中 */
    val packageStatus: Int? = null,
)

/**
 * 面试通关包模块生成状态。
 */
data class PassPackageModuleStatusData(
    /** 模块类型，取值来自 InterviewPassPackageModuleTypeEnum */
    val moduleType: String? = null,
    /** 模块名称 */
    val moduleName: String? = null,
    /** 模块任务状态，取值来自 InterviewPassPackageTaskStatusEnum */
    val taskStatus: String? = null,
    /** 失败原因 */
    val failReason: String? = null,
)
