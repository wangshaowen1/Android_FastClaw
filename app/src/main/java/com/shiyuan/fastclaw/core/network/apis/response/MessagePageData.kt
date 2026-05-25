package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 查询历史消息分页响应数据
 *
 * 消息结构复用 [JobAgentMessage]（与「进入或恢复会话」一致）。
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class MessagePageData(
    /** 历史消息列表，按端侧展示顺序返回 */
    val messages: List<JobAgentMessage>? = null,

    /** 下一页向上翻页游标 */
    val nextBeforeCursor: String? = null,

    /** 是否还有更早消息：true 还有 / false 没有 */
    val hasMore: Boolean? = null,
)