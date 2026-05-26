package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 流式发送用户消息请求体（求职 Agent SSE，POST + text/event-stream）
 *
 * 断线重连时 reconnect=true，requestId 与 lastSeq 必填，服务端按 Redis Stream 增量恢复，
 * Redis 过期时按 MySQL 消息快照兜底。
 *
 * @param sessionId        候选人求职 Agent 会话 ID（必填）
 * @param conversationId   对话 ID，用于消息和 SSE 请求归属（必填）
 * @param clientMessageId  客户端消息 ID；发送新消息时必填，用于消息幂等
 * @param interactionType  交互类型，TEXT-自然语言输入，ACTION-卡片按钮或选项动作；为空时按 TEXT 兼容。
 * @param action           结构化动作参数；interactionType=ACTION 时必填。
 * @param content          用户输入文本；TEXT 交互必填，ACTION 交互可传用户可见选择文案，断线重连只补发时可以为空。
 * @param requestId        本次 SSE 请求 ID；断线重连时必填
 * @param reconnect        是否断线重连请求
 * @param lastSeq          客户端已接收的最后一条 SSE 事件序号
 */
data class JobAgentStreamBody(
    val sessionId: String?,
    val conversationId: String?,
    val clientMessageId: String? = null,
    val interactionType: String? = null,
    val action: AgentMsgAction? = null,
    val content: String? = null,
    val requestId: String? = null,
    val reconnect: Boolean = false,
    val lastSeq: Long? = null,
)

/**
 * 结构化动作参数；interactionType=ACTION 时必填。
 *
 * @param actionCode        动作编码，取值见 CandidateJobAgentClientActionEnum。
 * @param sourceMessageId   动作来源卡片消息 ID；用于置灰来源卡片和防重复提交。
 * @param questionQueueId   动作来源问题队列 ID；没有来源卡片消息 ID 时用于反查原问题卡。
 * @param actionPayloadJson 动作扩展 JSON，保存卡片 payload、按钮值或确认值。
 */
data class AgentMsgAction(
    val actionCode: String? = null,
    val sourceMessageId: Long? = null,
    val questionQueueId: Long? = null,
    val actionPayloadJson: String? = null
)