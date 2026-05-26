package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 查询通信会话响应数据（求职 Agent 实例 / 活跃对话 / 流式请求状态）
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class ConversationQueryData(
    /** 候选人求职 Agent 实例 ID */
    val candidateJobAgentId: String? = null,

    /** 通信会话 ID */
    val conversationId: String? = null,

    /** 求职 Agent 状态会话 ID */
    val sessionId: String? = null,

    /**
     * 通信会话状态
     * ACTIVE 进行中 / PAUSED 暂停 / CLOSED 已关闭
     */
    val conversationStatus: String? = null,

    /**
     * 当前主阶段编码（CandidateJobAgentStageEnum）
     * PROFILE_BUILDING 画像建立 / JOB_MATCHING 找工作服务 / RECOVERABLE_ERROR 可恢复异常
     */
    val stageCode: String? = null,

    /** 当前节点编码（CandidateJobAgentNodeEnum） */
    val nodeCode: String? = null,

    /** 当前等待回答的问题业务 ID */
    val currentQuestionId: String? = null,

    /** 最后一条消息 ID */
    val lastMessageId: String? = null,

    /** 最后一次流式请求 ID（可用于取消、断线重连） */
    val lastRequestId: String? = null,

    /**
     * 最后一次或指定流式请求状态
     * INIT 初始化 / STREAMING 处理中 / SUCCESS 成功 / FAILED 失败 / TIMEOUT 模型超时 / CANCELED 已取消
     */
    val lastRequestStatus: String? = null,

    /** 当前是否存在未结束的流式请求 */
    val hasStreamingRequest: Boolean? = null,

    /** 服务端最新消息游标 */
    val latestCursor: String? = null,

    /** 服务端已知的最后一条 SSE 事件序号（断线重连用） */
    val lastSeq: Long? = null,

    /** 当前输入框是否可用 */
    val inputEnabled: Boolean? = null,
)