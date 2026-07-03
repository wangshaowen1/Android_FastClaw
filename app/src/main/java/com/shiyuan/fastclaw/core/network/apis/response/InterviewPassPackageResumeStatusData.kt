package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 查询通关包简历状态响应数据
 */
data class InterviewPassPackageResumeStatusData(
    /** 是否存在可用简历 */
    val hasResume: Boolean? = null,

    /** 可用简历材料检测 ID，后续创建支付订单时作为 resumeCheckId 使用 */
    val resumeCheckId: Long? = null,

    /** 简历文件名 */
    val resumeFileName: String? = null,

    /** 下一步动作：UPLOAD_RESUME-上传简历，UPLOAD_JD-上传 JD */
    val nextAction: String? = null,
)
