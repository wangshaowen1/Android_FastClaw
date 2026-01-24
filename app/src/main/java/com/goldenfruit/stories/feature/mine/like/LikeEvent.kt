package com.kgmedia.fastfilm.feature.mine.like

import com.kgmedia.fastfilm.core.network.apis.entity.MyFavoriteScriptInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:喜欢
 * @Data: 2025/9/11/周四  17:25
 * @Version: 1.0
 * *****************************************
 */
sealed class LikeEvent {
    //查看
    class ItemClick(val myFavoriteScriptInfo: MyFavoriteScriptInfo?) : LikeEvent()
}