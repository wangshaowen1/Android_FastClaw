package com.kgmedia.fastfilm.core.network.apis.response

/**
 * 查询用户未读消息数量
 */
data class MessagesUnreadNumData(
    /**
     * 消息未读数量总数
     */
    val unreadNumTotal: Int?,
    /**
     * 喜欢消息未读数量
     */
    val likesUnreadNum: Int?,
    /**
     * 追剧(短剧剧集)消息未读数量
     */
    val watchingUnreadNum: Int?,
    /**
     * 系统消息未读数量
     */
    val systemUnreadNum: Int?
)
