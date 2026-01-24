package com.kgmedia.fastfilm.feature.mine.chase

import com.kgmedia.fastfilm.core.network.apis.entity.TheTvSeriesIFollowInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:追剧
 * @Data: 2025/9/11/周四  17:25
 * @Version: 1.0
 * *****************************************
 */
sealed class BingeWatchEvent {
    //查看
    class ItemClick(val theTvSeriesIFollowInfo: TheTvSeriesIFollowInfo?) : BingeWatchEvent()
}