package com.shiyuan.fastclaw.feature.message.data

import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_CONVERSATION
import com.shiyuan.fastclaw.feature.message.MessageAdapter.Companion.TYPE_UNSUITABLE_ENTRY

/**
 * 消息列表统一行模型，供多类型 Adapter 根据 itemType 分发不同布局。
 */
sealed interface MessageListItem {
    /** 列表项稳定 id，用于 DiffUtil 判断是否为同一条数据。 */
    val id: String

    /** 列表项布局类型，对应 MessageAdapter 中注册的 TYPE 常量。 */
    val itemType: Int
}

/**
 * 普通 IM 会话行，展示联系人信息、末条消息、时间和未读状态。
 *
 * @param id 云信 conversationId，作为列表稳定 id 和后续进入会话的凭据。
 * @param contactId P2P 会话对端账号，用于资料查询、黑名单判断和发送消息。
 * @param targetName 会话对象昵称，资料缺失时使用账号兜底。
 * @param targetDesc 会话对象补充描述，当前为空时隐藏。
 * @param targetAvatar 会话对象头像地址。
 * @param lastMessage 会话末条消息摘要。
 * @param lastMessageTime 会话末条消息时间戳。
 * @param unreadCount 会话未读数量。
 * @param localExtension 会话本地扩展，仅当前设备可见。
 * @param serverExtension 会话服务端扩展，多端同步后可用于承载职位等业务信息。
 * @param isDraft 是否展示草稿标记，当前预留给后续草稿能力。
 */
data class ConversationItem(
    override val id: String,
    val contactId: String,
    val targetName: String,
    val targetDesc: String?,
    val targetAvatar: String?,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val localExtension: String?,
    val serverExtension: String?,
    val isDraft: Boolean = false,
    override val itemType: Int = TYPE_CONVERSATION
) : MessageListItem

/**
 * 已标记不合适的会话入口行，用于和普通会话在同一个消息列表中混排。
 *
 * @param count 当前入口下的联系人数量。
 */
data class UnsuitableEntry(
    val count: Int,
    override val id: String = "marked_unsuitable",
    override val itemType: Int = TYPE_UNSUITABLE_ENTRY

) : MessageListItem

