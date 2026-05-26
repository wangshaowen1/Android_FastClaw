package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 提交简历上传结果请求体（求职 Agent）
 *
 * resumeId / resumeTaskId / resumeFileUrl 用于衔接端侧已有的简历上传与解析能力，按实际上传方式回传其一。
 *
 * @param sessionId       候选人求职 Agent 会话 ID（必填）
 * @param conversationId  对话 ID，用于消息、任务和事件关联（必填）
 * @param clientActionId  客户端动作 ID，用于上传提交幂等（必填）
 * @param resumeId        已上传简历 ID；用于衔接现有简历上传能力
 * @param resumeTaskId    简历解析任务 ID；用于衔接已有解析任务状态
 * @param resumeFileUrl   简历文件地址或 OSS key；仅在端侧先上传文件后回传时使用
 */
data class ResumeUploadSubmitBody(
    val sessionId: String?,
    val conversationId: String?,
    val clientActionId: String?,
    val resumeId: String? = null,
    val resumeTaskId: String? = null,
    val resumeFileUrl: String? = null,
)
/**
 * 提交简历上传我的
 *
*/
 class ResumeUploadSubmitMyBody(
    val clientUploadId: String? = null,
    val resumeFileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val replaceResumeTaskId: String? = null,
    val replaceResumeId: String? = null,
)