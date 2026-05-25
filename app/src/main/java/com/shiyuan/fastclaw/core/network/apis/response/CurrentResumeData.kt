package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 查询当前可确认简历响应数据
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class CurrentResumeData(
    /** 当前账号是否存在可确认简历：true 存在 / false 不存在 */
    val hasCurrentResume: Boolean? = null,

    /** 当前可确认简历 ID */
    val resumeId: String? = null,

    /** 当前可确认简历名称 */
    val resumeName: String? = null,

    /** 简历最近更新时间，格式 yyyy-MM-dd HH:mm:ss */
    val resumeUpdateTime: String? = null,

    /** 简历轻量摘要，用于端侧确认是否本人简历 */
    val resumeSummary: String? = null,
)