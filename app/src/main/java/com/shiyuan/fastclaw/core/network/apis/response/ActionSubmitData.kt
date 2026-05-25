package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 提交卡片或按钮动作响应数据
 *
 * 消息结构复用 [JobAgentMessage]（与「进入或恢复会话」一致）。
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class ActionSubmitData(
    /** 候选人求职 Agent 会话 ID */
    val sessionId: String? = null,

    /** 对话 ID */
    val conversationId: String? = null,

    /** 动作是否被服务端接收：true 已接收并进入处理 / false 未接收或被业务校验拒绝 */
    val accepted: Boolean? = null,

    /** 动作处理后的最新消息游标 */
    val latestCursor: String? = null,

    /** 本次动作产生或更新的消息列表 */
    val messages: List<JobAgentMessage>? = null,
)