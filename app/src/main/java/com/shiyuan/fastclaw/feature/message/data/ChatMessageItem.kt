package com.shiyuan.fastclaw.feature.message.data

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.shiyuan.fastclaw.R

/**
 * IM 对话消息统一行模型，不感知 RecyclerView 的具体 viewType。
 */
sealed interface ChatMessageItem {
    /** 消息稳定 id，用于 DiffUtil 判断是否为同一条消息。 */
    val id: String

    /** 消息时间戳，用于列表排序和后续时间展示。 */
    val time: Long

    /** 消息时间展示文案，空字符串表示当前消息不展示时间。 */
    val timeText: String

    /** DataBinding 使用的时间区域展示状态。 */
    val hasTimeText: Boolean
        get() = timeText.isNotBlank()
}

/** 气泡消息方向，只描述业务语义，不与左右布局绑定。 */
enum class ChatMessageDirection {
    INCOMING,
    OUTGOING,
}

/**
 * 气泡内容类型。
 *
 * 新增音频、视频等消息时在此增加内容类型，并提供一个对应的 ChatContentProvider；
 * 不需要再创建 Incoming/Outgoing 两套 ViewHolder。
 */
enum class ChatMessageContentType(val stableId: Int) {
    TEXT(1),
    RESUME_REQUEST(2),
    RESUME_DELIVERY(3),
}

/**
 * 使用公共方向壳层渲染的消息。头像、时间等公共信息由壳层负责，具体内容交给 Provider。
 */
sealed interface BubbleChatItem : ChatMessageItem {
    val avatar: String?
    val direction: ChatMessageDirection
    val contentType: ChatMessageContentType
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
    /** 实际发起职业卡片事件的一方。 */
    val operatorRole: String?,
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
    /** 公司融资阶段。 */
    val financingStage: String?,
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
) : ChatMessageItem {
    /** 品牌名为空白时回退到公司名，与重构前的展示规则一致。 */
    val displayCompanyName: String?
        get() = brandName?.takeIf { it.isNotBlank() } ?: companyName

    /** 融资阶段为空白时不参与展示。 */
    val displayFinancingStage: String?
        get() = financingStage?.takeIf { it.isNotBlank() }

    val hasSalary: Boolean
        get() = !salaryText.isNullOrBlank()

    val hasCompany: Boolean
        get() = !displayCompanyName.isNullOrBlank()

    val hasFinancingStage: Boolean
        get() = !displayFinancingStage.isNullOrBlank()

    val hasCompanyInfo: Boolean
        get() = hasCompany || hasFinancingStage
}

/**
 * 文本消息行模型，区分左右气泡并展示普通文本内容。
 */
data class TextChatItem(
    /** 消息稳定 id，用于 DiffUtil 判断是否为同一条消息。 */
    override val id: String,
    /** 文本消息内容。 */
    val content: String,
    /** 消息发送方头像。 */
    override val avatar: String?,
    /** 消息方向。 */
    override val direction: ChatMessageDirection,
    /** 消息创建时间。 */
    override val time: Long,
    /** 消息时间展示文案。 */
    override val timeText: String,
) : BubbleChatItem {
    override val contentType: ChatMessageContentType = ChatMessageContentType.TEXT
}

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
    /** 云信索要简历原消息服务端 ID。 */
    val messageServerId: String?,
    /** 当前请求状态。 */
    val status: String?,
    /** 卡片标题。 */
    val title: String,
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
    /** 消息发送方头像。 */
    override val avatar: String?,
    /** 消息方向。 */
    override val direction: ChatMessageDirection,
    /** 消息创建时间。 */
    override val time: Long,
    /** 消息时间展示文案。 */
    override val timeText: String,
) : BubbleChatItem {
    override val contentType: ChatMessageContentType = ChatMessageContentType.RESUME_REQUEST

    /** 待处理状态需要展示同意、拒绝操作。 */
    val isPending: Boolean
        get() = status == STATUS_PENDING || status.isNullOrBlank()

    /** 只有收到的待处理请求允许当前用户操作。 */
    val canHandle: Boolean
        get() = direction == ChatMessageDirection.INCOMING && isPending

    /** 状态文案直接供 Item DataBinding 使用。 */
    @get:StringRes
    val statusTextRes: Int
        get() = when (status) {
            STATUS_ACCEPTED, STATUS_SENT -> R.string.message_chat_resume_request_sent
            STATUS_REJECTED -> R.string.message_chat_resume_request_rejected
            else -> if (direction == ChatMessageDirection.OUTGOING) {
                R.string.message_chat_resume_request_sent
            } else {
                R.string.message_chat_resume_request_pending
            }
        }

    private companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_SENT = "sent"
    }
}

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
    /** 消息发送方头像。 */
    override val avatar: String?,
    /** 消息方向。 */
    override val direction: ChatMessageDirection,
    /** 消息创建时间。 */
    override val time: Long,
    /** 消息时间展示文案。 */
    override val timeText: String,
) : BubbleChatItem {
    override val contentType: ChatMessageContentType = ChatMessageContentType.RESUME_DELIVERY

    /** 状态文案直接供 Item DataBinding 使用。 */
    @get:StringRes
    val statusTextRes: Int
        get() = when (status) {
            STATUS_ACCEPTED, STATUS_RECEIVED -> R.string.message_chat_resume_delivery_received
            STATUS_REJECTED -> R.string.message_chat_resume_delivery_rejected
            else -> R.string.message_chat_resume_delivery_sent
        }

    /** 待发送或已发送保持强调色，终态使用次要色。 */
    @get:ColorRes
    val statusColorRes: Int
        get() = if (status == STATUS_PENDING || status == STATUS_SENT || status.isNullOrBlank()) {
            R.color.color_2f6bff
        } else {
            R.color.color_75_3c3c43
        }

    private companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_SENT = "sent"
        const val STATUS_RECEIVED = "received"
    }
}
