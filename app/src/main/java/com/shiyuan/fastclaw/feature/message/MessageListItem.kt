package com.shiyuan.fastclaw.feature.message

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
 * @param id 会话联系人账号，作为列表稳定 id。
 * @param targetName 会话对象昵称，资料缺失时使用账号兜底。
 * @param targetDesc 会话对象补充描述，当前为空时隐藏。
 * @param targetAvatar 会话对象头像地址。
 * @param lastMessage 会话末条消息摘要。
 * @param lastMessageTime 会话末条消息时间戳。
 * @param unreadCount 会话未读数量。
 * @param isDraft 是否展示草稿标记，当前预留给后续草稿能力。
 */
data class MessageConversationItem(
    override val id: String,
    val targetName: String,
    val targetDesc: String?,
    val targetAvatar: String?,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isDraft: Boolean = false,
) : MessageListItem {
    override val itemType: Int = MessageAdapter.TYPE_CONVERSATION
}

/**
 * 已标记不合适的会话入口行，用于和普通会话在同一个消息列表中混排。
 *
 * @param count 当前入口下的联系人数量。
 */
data class UnsuitableConversationItem(
    val count: Int,
) : MessageListItem {
    override val id: String = "marked_unsuitable"
    override val itemType: Int = MessageAdapter.TYPE_MARKED_UNSUITABLE
}
