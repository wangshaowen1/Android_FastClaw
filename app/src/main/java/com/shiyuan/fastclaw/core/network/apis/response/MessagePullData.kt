package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 拉取增量消息响应数据
 *
 * 消息结构复用 [JobAgentMessage]（与「进入或恢复会话」一致）。
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class MessagePullData(
    /** 增量消息列表 */
    val messages: List<JobAgentMessage>? = null,

    /** 服务端最新消息游标 */
    val latestCursor: String? = null,

    /** 是否还有未拉取消息：true 还有 / false 没有 */
    val hasMore: Boolean? = null,
)