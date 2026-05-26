package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 提交卡片或按钮动作请求体（求职 Agent）
 *
 * @param sessionId          候选人求职 Agent 会话 ID（必填）
 * @param conversationId     对话 ID，用于消息、事件和 SSE 请求关联（必填）
 * @param clientActionId     客户端动作 ID，用于按钮、卡片、toast 操作幂等（必填）
 * @param actionCode         动作编码（必填，端侧动作白名单 CandidateJobAgentClientActionEnum）：
 *                           RESUME_UPLOAD_CLICK 点击上传简历入口 / RESUME_CONFIRM_YES 确认历史简历是本人 /
 *                           RESUME_CONFIRM_NO 否认历史简历是本人 / RESUME_REUPLOAD_CLICK 上传失败后重新上传 /
 *                           NO_RESUME_START_QUESTIONING 无简历直接进入核心补问 / SKIP_CURRENT_QUESTION 跳过当前问题 /
 *                           ATTRIBUTE_CONFIRM 确认属性候选值 / ATTRIBUTE_REJECT 拒绝属性候选值 /
 *                           RETRY_CURRENT_ACTION 重试当前动作
 * @param messageId          动作来源消息 ID
 * @param questionQueueId    动作来源问题队列 ID
 * @param actionPayloadJson  动作扩展 JSON，保存卡片 payload、按钮值或确认值
 */
data class ActionSubmitBody(
    val sessionId: String?,
    val conversationId: String?,
    val clientActionId: String?,
    val actionCode: String?,
    val messageId: String? = null,
    val questionQueueId: String? = null,
    val actionPayloadJson: String? = null,
)