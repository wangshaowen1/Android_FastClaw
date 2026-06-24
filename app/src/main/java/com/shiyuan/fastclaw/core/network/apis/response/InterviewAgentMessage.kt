package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 模拟面试 SSE 消息数据。
 */
data class InterviewAgentMessage(
    /** 消息 ID，服务端正式消息会返回 */
    val messageId: String? = null,

    /** 模拟面试会话 ID */
    val interviewId: String? = null,

    /** 面试间消息序号，用于断线恢复和重复消息过滤 */
    val messageSeq: Long? = null,

    /** 消息类型：ROOM_STATE_CHANGED / TEXT_DELTA / TEXT_DONE / TEXT_BLOCKED / INTERVIEW_FINISHED / ERROR */
    val messageType: String? = null,

    /** 发送方类型：AGENT / USER */
    val senderType: String? = null,

    /** 消息文本内容，TEXT_DELTA / TEXT_DONE / TEXT_BLOCKED 等文本消息使用 */
    val content: String? = null,

    /** 顶层房间状态，和 payload.roomState 含义一致；页面状态判断优先使用 payload.roomState */
    val roomState: String? = null,

    /** 错误码，ERROR 消息使用 */
    val errorCode: String? = null,

    /** 错误提示文案，ERROR 消息使用 */
    val errorMessage: String? = null,

    /** 错误是否可恢复，ERROR 消息使用 */
    val recoverable: Boolean? = null,

    /** 不同消息类型的扩展数据 */
    val payload: InterviewAgentPayload? = null,
)

/**
 * 模拟面试 SSE 消息 payload。
 *
 * 不同 messageType 共用一个结构，字段按联调文档中 ROOM_STATE_CHANGED、TEXT_DELTA、
 * TEXT_DONE、TEXT_BLOCKED、INTERVIEW_FINISHED、ERROR 的 payload 合并。
 */
data class InterviewAgentPayload(
    /** 恢复动作：RESTORE_ROOM / RESUBMIT_ANSWER / RETURN_TO_CHAT / SESSION_CLOSED */
    val recoverAction: String? = null,

    /** 面试会话状态：PREPARING / INTERVIEWING / REPORTING / COMPLETED / CANCELED / FAILED */
    val sessionStatus: String? = null,

    /** 房间状态：WAITING_TARGET_JOB / TARGET_JOB_PARSING / TARGET_JOB_PARSE_SUCCESS / TARGET_JOB_PARSE_FAILED / PLAN_GENERATING / PLAN_READY / WAITING_USER_ANSWER / ANSWER_SUBMITTED / WAITING_FOLLOW_UP_DECISION / REPORT_GENERATING / FINISHED / CANCELED */
    val roomState: String? = null,

    /** 模拟面试会话 ID */
    val interviewId: String? = null,

    /** 小虾对话 ID，面试结束后回到对应对话页使用 */
    val conversationId: String? = null,

    /** 原入口消息 ID */
    val entryMessageId: String? = null,

    /** 会话关闭原因，例如 SESSION_RECOVER_TIMEOUT */
    val closedReason: String? = null,

    /** 服务端已恢复消息的最新序号 */
    val latestMessageSeq: Long? = null,

    /** 恢复现场时返回的历史消息列表 */
    val messages: List<InterviewAgentMessage>? = null,

    /** 恢复现场时的当前题目信息 */
    val currentQuestion: InterviewCurrentQuestion? = null,

    /** 当前题作答截止时间 */
    val answerDeadlineAt: String? = null,

    /** 当前题剩余秒数 */
    val remainingSeconds: Int? = null,

    /** 题目类型：OPENING / MAIN / FOLLOW_UP / CLOSING / USER_ANSWER */
    val questionType: String? = null,

    /** 题目 ID */
    val questionId: String? = null,

    /** 追问所属的主问题 ID */
    val rootQuestionId: String? = null,

    /** 当前题序号 */
    val questionIndex: Int? = null,

    /** 当前追问轮次 */
    val questionRound: Int? = null,

    /** 流式片段序号，TEXT_DELTA 使用 */
    val segmentIndex: Int? = null,

    /** 用户回答 ID */
    val answerId: String? = null,

    /** APP 提交回答时生成的幂等 ID */
    val answerSubmitId: String? = null,

    /** 回答结束原因：USER_SUBMIT / TIMEOUT_AUTO_SUBMIT / NO_ANSWER_SKIP / TECHNICAL_INTERRUPTED */
    val answerEndReason: String? = null,

    /** 错误提示文案 */
    val errorMessage: String? = null,
)

data class InterviewCurrentQuestion(
    /** 当前题目 ID */
    val questionId: String? = null,

    /** 当前题目类型 */
    val questionType: String? = null,

    /** 当前题目文本 */
    val content: String? = null,
)
