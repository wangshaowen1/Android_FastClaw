package com.kgmedia.fastfilm.feature.message

import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.request.BannerBody
import com.kgmedia.fastfilm.core.network.apis.request.MessagesTypeBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessagesRepository : AppRepository() {

    /**
     * 查询所有类型消息列表
     */
    suspend fun messagesSummary() = withContext(Dispatchers.IO) {
        api.messagesSummary(UserTokenBody())
    }

    /**
     * 消息全部设置成已读
     * @param messagesType 消息类型1=追剧消息2=喜欢消息3=系统消息
     */
    suspend fun messagesMarkRead(messagesType: Int?) = withContext(Dispatchers.IO) {
        api.messagesMarkRead(MessagesTypeBody(messagesType))
    }


    /**
     * 获取banner信息
     */
    suspend fun appBanner(bannerBody:BannerBody) = withContext(Dispatchers.IO) {
        api.getAppBanner(bannerBody)
    }
}