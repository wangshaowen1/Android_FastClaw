package com.shiyuan.fastclaw.core.network.apis.response

/**
 * 进入或恢复会话响应数据
 *
 * @createtime 2026年05月25日
 * @author TooWhiteT
 */
data class SessionEnterData(
    /** 候选人求职 Agent 会话 ID */
    val sessionId: String? = null,

    /** 当前对话 ID */
    val conversationId: String? = null,

    /**
     * 当前主阶段编码（CandidateJobAgentStageEnum）
     * PROFILE_BUILDING 求职画像建立 / JOB_MATCHING 找工作服务 / RECOVERABLE_ERROR 可恢复异常
     */
    val stageCode: String? = null,

    /**
     * 当前节点编码（CandidateJobAgentNodeEnum）
     * WELCOME 欢迎引导 / RESUME_PENDING 等待简历 / RESUME_CONFIRMING 简历确认中 /
     * RESUME_PARSING 简历解析中 / CORE_PROFILE_COLLECTING 核心画像补问中 / JOB_SEARCHING 找工作中 /
     * NO_JOB_TEMPORARILY 暂未找到 / EXTENDED_PROFILE_COLLECTING 扩展画像补问中 /
     * RESUME_UPDATING 简历更新中 / ERROR_RECOVERABLE 可恢复异常
     */
    val nodeCode: String? = null,

    /** 服务端最新消息游标 */
    val latestCursor: String? = null,

    /** 恢复返回的最近消息列表 */
    val messages: List<JobAgentMessage>? = null,

    /** 当前建议端侧展示的下一步动作 */
    val nextActions: List<JobAgentAction>? = null,

    /** 当前账号是否存在可确认简历 */
    val hasCurrentResume: Boolean? = null,
)

/**
 * 求职 Agent 会话消息
 */
data class JobAgentMessage(
    /** 消息 ID */
    val messageId: String? = null,

    /** 消息序号，同一会话内单调递增 */
    val sequenceNo: Long? = null,

    /** 消息游标，用于分页、增量拉取和断线恢复 */
    val cursor: String? = null,

    /**
     * 发送方类型（CandidateJobAgentMessageSenderTypeEnum）
     * USER 用户 / AGENT Agent / SYSTEM 系统
     */
    val senderType: String? = null,

    /**
     * 消息类型（CandidateJobAgentMessageTypeEnum）
     * TEXT 文本 / CARD 卡片 / TOAST Toast / ERROR 异常兜底
     */
    val messageType: String? = null,

    /**
     * 卡片类型（CandidateJobAgentCardTypeEnum）；非卡片消息为空
     * UPLOAD_RESUME 上传简历卡 / HISTORY_RESUME_CONFIRM 历史简历确认卡 /
     * CORE_QUESTION 核心补问卡 / ATTRIBUTE_CONFIRM 属性确认卡 / UNDERSTANDING_SUMMARY 理解度摘要卡
     */
    val cardType: String? = null,

    /** 消息文本内容 */
    val content: String? = null,

    /** 卡片或 toast 结构化 payload JSON */
    val payloadJson: String? = null,

    /** 响应组 ID，同一轮用户消息触发的多条 Agent 消息使用同一个响应组 */
    val responseGroupId: String? = null,

    /**
     * 消息生命周期阶段（CandidateJobAgentMessagePhaseEnum）
     * ACK 已收到 / PROCESSING 处理中 / RESULT 结果 / ACTION_REQUIRED 需要用户操作 / ERROR 失败
     */
    val messagePhase: String? = null,

    /**
     * 端侧展示策略（CandidateJobAgentDisplayPolicyEnum）
     * APPEND 追加 / KEEP 保留 / REPLACE_ON_TERMINAL 终态替换 /
     * DISMISS_ON_TERMINAL 终态移除 / REPLACE_TARGET 替换目标消息
     */
    val displayPolicy: String? = null,

    /**
     * 响应组状态（CandidateJobAgentResponseGroupStatusEnum）
     * RUNNING 运行中 / COMPLETED 已完成 / WAITING_USER 等待用户 / FAILED 失败 / CANCELED 已取消
     */
    val groupStatus: String? = null,

    /** 精确替换目标消息 ID；displayPolicy 为 REPLACE_TARGET 时使用 */
    val replaceTargetMessageId: String? = null,

    /**
     * 消息状态
     * PENDING 待处理 / STREAMING 流式生成中 / DONE 已完成 / FAILED 失败 / CANCELLED 已取消
     */
    val messageStatus: String? = null,

    /** 建议端侧展示的操作列表 */
    val suggestedActions: List<JobAgentAction>? = null,

    /** 消息创建时间，格式 yyyy-MM-dd HH:mm:ss */
    val createTime: String? = null,

    /** 已有对话 ID；为空表示服务端恢复或创建 */
    val conversationId: String? = null,
)

data class PayLoadBean(
    val interactionState: InteractionStateBean? = null,
    val questionQueueId: Long? = null,
    val questionType: String? = null,
    val questionText: String? = null,
    val attributeGroupCode: String? = null,
    val metaCode: String? = null,
)

data class InteractionStateBean(
    /**
     * ANSWERED or ACTIVE
     */
    val status: String? = null,
    /**
     * true  or  false
     */
    val disabled: Boolean? = null,
)

/**
 * 求职 Agent 建议动作（suggestedActions / nextActions）
 */
data class JobAgentAction(
    /**
     * 动作编码，提交时回传 actionCode（CandidateJobAgentClientActionEnum）
     * RESUME_UPLOAD_CLICK 点击上传简历入口 / RESUME_CONFIRM_YES 确认历史简历是本人 /
     * RESUME_CONFIRM_NO 否认历史简历是本人 / RESUME_REUPLOAD_CLICK 上传失败后重新上传 /
     * NO_RESUME_START_QUESTIONING 无简历直接进入核心补问 / SKIP_CURRENT_QUESTION 跳过当前问题 /
     * RETRY_CURRENT_ACTION 重试当前动作
     */
    val actionCode: String? = null,

    /** 动作展示文案 */
    val actionText: String? = null,

    /** 动作是否禁用 */
    val disabled: Boolean? = null,

    /** 动作扩展 payload JSON */
    val payloadJson: String? = null,
)