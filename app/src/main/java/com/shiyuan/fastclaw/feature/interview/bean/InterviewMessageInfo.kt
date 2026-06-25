package com.shiyuan.fastclaw.feature.interview.bean

/**
 * 模拟面试房间消息展示模型。
 *
 * 用于端侧消息列表渲染，保留本地消息 ID 和回答提交 ID，方便流式消息更新及内容安全拦截回填。
 */
data class InterviewMessageInfo(
    /** 本地消息 ID，用于 RecyclerView diff 和流式消息覆盖更新 */
    val id: String? = null,

    /** 发送方类型，取值复用 InterviewViewModel.SENDER_AGENT / SENDER_USER */
    val senderType: String ?= null,

    /** 消息展示文本 */
    val content: String ?= null,

    /** 用户提交回答时生成的幂等 ID，用于服务端拦截消息回填匹配本地历史消息 */
    val answerSubmitId: String? = null
)

/**
 * 模拟面试房间状态。
 */
enum class InterviewRoomState {
    /** 端侧初始态，尚未收到服务端 roomState */
    STARTING,

    /** 等待选择目标岗位 */
    WAITING_TARGET_JOB,

    /** 目标岗位解析中 */
    TARGET_JOB_PARSING,

    /** 目标岗位解析成功 */
    TARGET_JOB_PARSE_SUCCESS,

    /** 目标岗位解析失败 */
    TARGET_JOB_PARSE_FAILED,

    /** 面试计划生成中 */
    PLAN_GENERATING,

    /** 面试计划已生成 */
    PLAN_READY,

    /** 等待用户作答 */
    WAITING_USER_ANSWER,

    /** 回答已提交 */
    ANSWER_SUBMITTED,

    /** 等待追问判断 */
    WAITING_FOLLOW_UP_DECISION,

    /** 报告生成中 */
    REPORT_GENERATING,

    /** 端侧本地作答中状态 */
    ANSWERING,

    /** 面试已结束 */
    FINISHED,

    /** 面试已取消 */
    CANCELED,

    /** 面试流程异常 */
    ERROR,
}
