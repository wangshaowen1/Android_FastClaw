package com.shiyuan.fastclaw.feature.interview.bean

data class InterviewRoomMessage(
    /** 本地消息 ID，用于 RecyclerView diff 和流式消息覆盖更新 */
    val id: String,

    /** 发送方类型，取值复用 InterviewViewModel.SENDER_AGENT / SENDER_USER */
    val senderType: String,

    /** 消息展示文本 */
    val content: String,
)

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
