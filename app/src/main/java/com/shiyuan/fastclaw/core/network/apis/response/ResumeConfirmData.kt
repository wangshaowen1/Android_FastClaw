package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 确认或否认当前简历响应数据
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class ResumeConfirmData(
    /** 服务端是否接收本次简历动作：true 已接收并进入处理 / false 未接收或被业务校验拒绝 */
    val accepted: Boolean? = null,

    /** 简历 ID */
    val resumeId: String? = null,

    /** 简历解析任务 ID */
    val resumeTaskId: String? = null,

    /** 处理后的最新消息游标 */
    val latestCursor: String? = null,
)