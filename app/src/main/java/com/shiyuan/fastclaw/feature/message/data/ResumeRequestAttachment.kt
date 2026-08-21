package com.shiyuan.fastclaw.feature.message.data

import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.feature.message.MessageFormatter.customPayload
import com.shiyuan.fastclaw.feature.message.MessageFormatter.toResumeMimeType
import com.tulele.im.custom.CustomAttachment
import com.tulele.im.entity.MessageWrapper
import org.json.JSONObject

// HR 索要简历的 IM 自定义附件，按跨端协议输出 attachment.raw。
class ResumeRequestAttachment : CustomAttachment(MSG_TYPE) {

    // 简历业务 ID，当前由最新简历接口的 checkId 映射。
    var resumeId: String? = null

    // 简历文件名称。
    var resumeName: String? = null

    // 简历文件地址。
    var resumeUrl: String? = null

    // 简历文件 MIME 类型。
    var resumeMimeType: String? = null

    // 简历文件大小，单位字节。
    var resumeSize: Long? = null

    // 业务创建时间，毫秒时间戳。
    var createdAt: Long = 0L

    // 业务更新时间，毫秒时间戳。
    var updatedAt: Long = 0L

    // 执行当前操作的云信账号 ID。
    var operatorId: String? = null

    // 协议版本号。
    var protocolVersion: Int? = null

    // 协议消息类型。
    var protocolType: String? = null

    // 当前请求状态。
    var status: String? = null

    /**
     * 返回通知栏和会话摘要降级文案。
     */
    override fun getPushContent(): CharSequence = when (status) {
        STATUS_ACCEPTED -> PUSH_ACCEPTED
        STATUS_REJECTED -> PUSH_REJECTED
        else -> PUSH_PENDING
    }

    /**
     * 序列化成协议约定的 attachment.raw JSON。
     */
    override fun toJson(send: Boolean): String {
        val resume = if (status == STATUS_ACCEPTED) {
            JSONObject()
                .put(KEY_RESUME_ID, resumeId)
                .put(KEY_NAME, resumeName)
                .put(KEY_URL, resumeUrl)
                .put(KEY_MIME_TYPE, resumeMimeType)
                .put(KEY_SIZE, resumeSize)
        } else {
            JSONObject.NULL
        }
        return JSONObject()
            .put(KEY_VERSION, protocolVersion ?: PROTOCOL_VERSION)
            .put(KEY_TYPE, TYPE_RESUME_REQUEST)
            .put(KEY_STATUS, status ?: STATUS_PENDING)
            .put(KEY_RESUME, resume)
            .put(KEY_CREATED_AT, createdAt)
            .put(KEY_UPDATED_AT, updatedAt)
            .put(KEY_OPERATOR_ID, operatorId)
            .toString()
    }

    companion object {
        // HR 索要简历自定义消息 subtype。
        const val MSG_TYPE = 1001

        private const val PROTOCOL_VERSION = 1
        private const val TYPE_RESUME_REQUEST = "resumeRequest"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_ACCEPTED = "accepted"
        private const val STATUS_REJECTED = "rejected"
        private const val STATUS_SENT = "sent"
        private const val PUSH_PENDING = "对方请求查看你的附件简历"
        private const val PUSH_ACCEPTED = "附件简历已发送"
        private const val PUSH_REJECTED = "你已拒绝发送附件简历"
        private const val KEY_VERSION = "v"
        private const val KEY_TYPE = "type"
        private const val KEY_STATUS = "status"
        private const val KEY_RESUME = "resume"
        private const val KEY_RESUME_ID = "resumeId"
        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_MIME_TYPE = "mimeType"
        private const val KEY_SIZE = "size"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_UPDATED_AT = "updatedAt"
        private const val KEY_OPERATOR_ID = "operatorId"

        /**
         * 根据原索要简历卡片和最新简历数据组装已同意附件。
         */
        fun buildAccepted(item: ResumeRequestChatItem, resume: ResumeData): ResumeRequestAttachment {
            val now = System.currentTimeMillis()
            return ResumeRequestAttachment().apply {
                protocolVersion = item.protocolVersion ?: PROTOCOL_VERSION
                protocolType = TYPE_RESUME_REQUEST
                status = STATUS_ACCEPTED
                resumeId = resume.checkId
                resumeName = resume.resumeFileName
                resumeUrl = resume.resumeFileUrl
                resumeMimeType = resume.resumeFileType.toResumeMimeType()
                resumeSize = resume.resumeFileSize
                createdAt = item.createdAt ?: now
                updatedAt = now
                operatorId = User.get().imAccessId
            }
        }

        /**
         * 将 HR 索要简历消息转换成聊天列表展示模型。
         */
        fun buildChat(message: MessageWrapper, isSelf: Boolean, timeText: String, avatar: String?): ResumeRequestChatItem {
            val payload = message.customPayload()
            val resume = payload?.optJSONObject(KEY_RESUME)
            val status = payload?.optString(KEY_STATUS)
            return ResumeRequestChatItem(
                id = message.messageClientId,
                protocolVersion = payload?.optInt(KEY_VERSION),
                protocolType = payload?.optString(KEY_TYPE),
                requestId = payload?.optString("requestId"),
                status = status,
                title = App.instance.getString(R.string.message_chat_resume_request_title),
                resumeId = resume?.optString(KEY_RESUME_ID),
                resumeName = resume?.optString(KEY_NAME),
                resumeUrl = resume?.optString(KEY_URL),
                resumeMimeType = resume?.optString(KEY_MIME_TYPE),
                resumeSize = resume?.optLong(KEY_SIZE),
                createdAt = payload?.optLong(KEY_CREATED_AT),
                updatedAt = payload?.optLong(KEY_UPDATED_AT),
                operatorId = payload?.optString(KEY_OPERATOR_ID),
                avatar = avatar,
                isSelf = isSelf,
                time = message.time,
                timeText = timeText,
            )
        }
    }
}
