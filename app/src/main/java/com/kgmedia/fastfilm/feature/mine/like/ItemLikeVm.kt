package com.kgmedia.fastfilm.feature.mine.like

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.MyFavoriteScriptInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:喜欢
 * @Data: 2025/11/4/周二  13:47
 * @Version: 1.0
 * *****************************************
 */
class ItemLikeVm (
    private val eventVm: ItemEventsViewModel<LikeEvent>,
    myFavoriteScriptInfo: MyFavoriteScriptInfo?
) : AppItemViewModel<MyFavoriteScriptInfo>(myFavoriteScriptInfo){
    fun onItemClick() {
        eventVm.postEvent(LikeEvent.ItemClick(data.get()))
    }
}