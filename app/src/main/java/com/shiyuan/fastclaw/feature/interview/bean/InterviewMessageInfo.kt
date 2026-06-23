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
    /** 初始进入页面，尚未完成面试会话准备 */
    STARTING,

    /** 面试问题或房间信息准备中 */
    PREPARING,

    /** 等待用户上传或选择目标岗位 */
    WAITING_TARGET_JOB,

    /** 目标岗位截图解析中 */
    TARGET_JOB_PARSING,

    /** 面试中，等待用户点击开始作答 */
    INTERVIEWING,

    /** 用户正在作答中 */
    ANSWERING,

    /** 面试已结束 */
    FINISHED,

    /** 面试流程异常 */
    ERROR,
}
