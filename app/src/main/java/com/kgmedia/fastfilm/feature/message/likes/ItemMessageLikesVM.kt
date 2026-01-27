package com.kgmedia.fastfilm.feature.message.likes

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageLikeInfo
import com.kgmedia.fastfilm.feature.play.DramasPlayPagerActivity

/**
 * 消息item
 */
class ItemMessageLikesVM(info: SystemMessageLikeInfo) :
    AppItemViewModel<SystemMessageLikeInfo>(info) {
    fun onClick() {
        if (data.get()?.likeTargetType == 1) {
            DramasPlayPagerActivity.start(data.get()?.likeTargetBusId)
        }
    }
}