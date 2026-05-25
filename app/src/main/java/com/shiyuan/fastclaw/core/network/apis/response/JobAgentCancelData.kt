package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 取消流式生成响应数据
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class JobAgentCancelData(
    /** 本次取消的流式请求 ID */
    val requestId: String? = null,

    /** 对话 ID */
    val conversationId: String? = null,

    /** 服务端是否接收取消请求：true 已接收 / false 未接收或请求已终态 */
    val accepted: Boolean? = null,

    /**
     * 取消后的请求状态
     * CANCELING 取消中 / CANCELED 已取消 / SUCCESS 已成功 / FAILED 已失败 / TIMEOUT 模型超时
     */
    val requestStatus: String? = null,
)