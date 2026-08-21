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
import java.util.UUID

// 求职者主动发送简历的 IM 自定义附件，按跨端协议输出 attachment.raw。
class ResumeDeliveryAttachment : CustomAttachment(MSG_TYPE) {

    // 主动投递业务幂等 ID。
    var deliveryId: String? = null

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

    // 执行当前发送操作的云信账号 ID。
    var operatorId: String? = null

    // 协议版本号。
    var protocolVersion: Int? = null

    // 协议消息类型。
    var protocolType: String? = null

    // 当前投递状态。
    var status: String? = null

    /**
     * 返回通知栏和会话摘要降级文案。
     */
    override fun getPushContent(): CharSequence = resumeName.orEmpty()

    /**
     * 序列化成协议约定的 attachment.raw JSON。
     */
    override fun toJson(send: Boolean): String {
        val resume = JSONObject()
            .put(KEY_RESUME_ID, resumeId)
            .put(KEY_NAME, resumeName)
            .put(KEY_URL, resumeUrl)
            .put(KEY_MIME_TYPE, resumeMimeType)
            .put(KEY_SIZE, resumeSize)
        return JSONObject()
            .put(KEY_VERSION, PROTOCOL_VERSION)
            .put(KEY_TYPE, TYPE_RESUME_DELIVERY)
            .put(KEY_DELIVERY_ID, deliveryId)
            .put(KEY_STATUS, status ?: STATUS_PENDING)
            .put(KEY_RESUME, resume)
            .put(KEY_CREATED_AT, createdAt)
            .put(KEY_UPDATED_AT, updatedAt)
            .put(KEY_OPERATOR_ID, operatorId)
            .toString()
    }

    companion object {
        // 主动发送简历自定义消息 subtype。
        const val MSG_TYPE = 1002

        private const val PROTOCOL_VERSION = 1
        private const val TYPE_RESUME_DELIVERY = "resumeDelivery"
        private const val STATUS_PENDING = "pending"
        private const val KEY_VERSION = "v"
        private const val KEY_TYPE = "type"
        private const val KEY_DELIVERY_ID = "deliveryId"
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
         * 根据最新简历接口数据组装主动发送简历附件。
         */
        fun buildResume(resume: ResumeData): ResumeDeliveryAttachment {
            val now = System.currentTimeMillis()
            return ResumeDeliveryAttachment().apply {
                protocolVersion = PROTOCOL_VERSION
                protocolType = TYPE_RESUME_DELIVERY
                deliveryId = UUID.randomUUID().toString()
                status = STATUS_PENDING
                resumeId = resume.checkId
                resumeName = resume.resumeFileName
                resumeUrl = resume.resumeFileUrl
                resumeMimeType = resume.resumeFileType.toResumeMimeType()
                resumeSize = resume.resumeFileSize
                createdAt = now
                updatedAt = now
                operatorId = User.get().imAccessId
            }
        }

        /**
         * 将主动发送简历消息转换成聊天列表展示模型。
         */
        fun buildChat(message: MessageWrapper, isSelf: Boolean, timeText: String): ResumeDeliveryChatItem {
            val payload = message.customPayload()
            val resume = payload?.optJSONObject(KEY_RESUME)
            val status = payload?.optString(KEY_STATUS)
            val defaultFileName = App.instance.getString(R.string.message_chat_resume_file_default)
            return ResumeDeliveryChatItem(
                id = message.messageClientId,
                protocolVersion = payload?.optInt(KEY_VERSION),
                protocolType = payload?.optString(KEY_TYPE),
                deliveryId = payload?.optString(KEY_DELIVERY_ID),
                status = status,
                title = App.instance.getString(R.string.message_chat_resume_delivery_title),
                fileName = resume?.optString(KEY_NAME) ?: defaultFileName,
                resumeId = resume?.optString(KEY_RESUME_ID),
                resumeUrl = resume?.optString(KEY_URL),
                resumeMimeType = resume?.optString(KEY_MIME_TYPE),
                resumeSize = resume?.optLong(KEY_SIZE),
                createdAt = payload?.optLong(KEY_CREATED_AT),
                updatedAt = payload?.optLong(KEY_UPDATED_AT),
                operatorId = payload?.optString(KEY_OPERATOR_ID),
                avatar = if (isSelf) User.get().avatarUrl else null,
                isSelf = isSelf,
                time = message.time,
                timeText = timeText,
            )
        }
    }
}
