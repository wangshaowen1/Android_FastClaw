package com.shiyuan.fastclaw.feature.message.data

import com.shiyuan.fastclaw.feature.message.chat.MessageChatAdapter.Companion.TYPE_RESUME_DELIVERY
import com.shiyuan.fastclaw.feature.message.chat.MessageChatAdapter.Companion.TYPE_RESUME_REQUEST
import com.shiyuan.fastclaw.feature.message.chat.MessageChatAdapter.Companion.TYPE_TEXT_LEFT
import com.shiyuan.fastclaw.feature.message.chat.MessageChatAdapter.Companion.TYPE_TEXT_RIGHT
import com.shiyuan.fastclaw.feature.message.chat.MessageChatAdapter.Companion.TYPE_JOB_CARD

/**
 * IM 对话消息统一行模型，供多类型 Adapter 根据 itemType 分发不同布局。
 */
sealed interface ChatMessageItem {
    /** 消息稳定 id，用于 DiffUtil 判断是否为同一条消息。 */
    val id: String

    /** 消息布局类型，对应 MessageChatAdapter 中注册的 TYPE 常量。 */
    val itemType: Int

    /** 消息时间戳，用于列表排序和后续时间展示。 */
    val time: Long

    /** 消息时间展示文案，空字符串表示当前消息不展示时间。 */
    val timeText: String
}

/**
 * 职位卡片消息模型，用于展示本次 IM 会话关联的岗位信息。
 */
data class JobChatItem(
    /** 消息稳定 id，用于 DiffUtil 判断是否为同一条消息。 */
    override val id: String,
    /** 协议版本号。 */
    val protocolVersion: Int?,
    /** 协议消息类型。 */
    val protocolType: String?,
    /** 职位卡片业务幂等 ID。 */
    val cardId: String?,
    /** 职位业务 ID。 */
    val jobId: String?,
    /** 职位名称快照。 */
    val jobName: String?,
    /** 薪资展示快照。 */
    val salaryText: String?,
    /** 品牌业务 ID。 */
    val brandId: String?,
    /** 品牌名称。 */
    val brandName: String?,
    /** 公司业务 ID。 */
    val companyId: String?,
    /** 公司名称快照。 */
    val companyName: String?,
    /** 城市编码。 */
    val cityCode: String?,
    /** 城市名称。 */
    val cityName: String?,
    /** 经验枚举值。 */
    val experienceCode: String?,
    /** 经验展示文案。 */
    val experienceText: String?,
    /** 学历枚举值。 */
    val educationCode: String?,
    /** 学历展示文案。 */
    val educationText: String?,
    /** 是否为更换后的沟通职位。 */
    val isJobChanged: Boolean,
    /** 消息创建时间。 */
    override val time: Long,
    /** 消息时间展示文案。 */
    override val timeText: String,
    /** 消息布局类型。 */
    override val itemType: Int = TYPE_JOB_CARD,
) : ChatMessageItem

/**
 * 文本消息行模型，区分左右气泡并展示普通文本内容。
 */
data class TextChatItem(
    /** 消息稳定 id，用于 DiffUtil 判断是否为同一条消息。 */
    override val id: String,
    /** 文本消息内容。 */
    val content: String,
    /** 消息发送方头像。 */
    val avatar: String?,
    /** 是否为当前登录用户发送。 */
    val isSelf: Boolean,
    /** 消息创建时间。 */
    override val time: Long,
    /** 消息时间展示文案。 */
    override val timeText: String,
    /** 消息布局类型。 */
    override val itemType: Int = if (isSelf) TYPE_TEXT_RIGHT else TYPE_TEXT_LEFT,
) : ChatMessageItem

/**
 * 简历请求卡片消息模型，用于 HR 向求职者发起查看简历请求。
 */
data class ResumeRequestChatItem(
    /** 消息稳定 id，用于 DiffUtil 判断是否为同一条消息。 */
    override val id: String,
    /** 协议版本号。 */
    val protocolVersion: Int?,
    /** 协议消息类型。 */
    val protocolType: String?,
    /** 简历请求业务幂等 ID。 */
    val requestId: String?,
    /** 当前请求状态。 */
    val status: String?,
    /** 卡片标题。 */
    val title: String,
    /** 当前请求状态展示文案。 */
    val statusText: String,
    /** 简历业务 ID。 */
    val resumeId: String?,
    /** 简历文件名。 */
    val resumeName: String?,
    /** 简历访问地址。 */
    val resumeUrl: String?,
    /** 简历 MIME 类型。 */
    val resumeMimeType: String?,
    /** 简历文件大小。 */
    val resumeSize: Long?,
    /** 业务创建时间。 */
    val createdAt: Long?,
    /** 业务更新时间。 */
    val updatedAt: Long?,
    /** 最近操作人云信账号。 */
    val operatorId: String?,
    /** 是否为当前登录用户发送。 */
    val isSelf: Boolean,
    /** 消息创建时间。 */
    override val time: Long,
    /** 消息时间展示文案。 */
    override val timeText: String,
    /** 消息布局类型。 */
    override val itemType: Int = TYPE_RESUME_REQUEST,
) : ChatMessageItem

/**
 * 简历投递卡片消息模型，用于展示已发送或已接收的简历文件。
 */
data class ResumeDeliveryChatItem(
    /** 消息稳定 id，用于 DiffUtil 判断是否为同一条消息。 */
    override val id: String,
    /** 协议版本号。 */
    val protocolVersion: Int?,
    /** 协议消息类型。 */
    val protocolType: String?,
    /** 简历投递业务幂等 ID。 */
    val deliveryId: String?,
    /** 当前投递状态。 */
    val status: String?,
    /** 卡片标题。 */
    val title: String,
    /** 简历文件名。 */
    val fileName: String,
    /** 当前投递状态展示文案。 */
    val statusText: String,
    /** 简历业务 ID。 */
    val resumeId: String?,
    /** 简历访问地址。 */
    val resumeUrl: String?,
    /** 简历 MIME 类型。 */
    val resumeMimeType: String?,
    /** 简历文件大小。 */
    val resumeSize: Long?,
    /** 业务创建时间。 */
    val createdAt: Long?,
    /** 业务更新时间。 */
    val updatedAt: Long?,
    /** 最近操作人云信账号。 */
    val operatorId: String?,
    /** 是否为当前登录用户发送。 */
    val isSelf: Boolean,
    /** 消息创建时间。 */
    override val time: Long,
    /** 消息时间展示文案。 */
    override val timeText: String,
    /** 消息布局类型。 */
    override val itemType: Int = TYPE_RESUME_DELIVERY,
) : ChatMessageItem
