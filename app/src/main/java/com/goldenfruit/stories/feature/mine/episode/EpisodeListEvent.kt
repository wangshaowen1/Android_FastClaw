package com.kgmedia.fastfilm.feature.mine.episode

import com.kgmedia.fastfilm.core.network.apis.entity.MyTvSeriesListInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:小说列表
 * @Data: 2025/9/11/周四  17:25
 * @Version: 1.0
 * *****************************************
 */
sealed class EpisodeListEvent {
    //查看
    class ItemClick(val myTvSeriesListInfo: MyTvSeriesListInfo?) : EpisodeListEvent()
    //上架
    class Launch(val myTvSeriesListInfo: MyTvSeriesListInfo?) : EpisodeListEvent()
    //分享
    class Share(val myTvSeriesListInfo: MyTvSeriesListInfo?) : EpisodeListEvent()
}