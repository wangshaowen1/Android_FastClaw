package com.kgmedia.fastfilm.feature.mine.episode

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.formatPlays
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.entity.MyTvSeriesListInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:剧集列表
 * @Data: 2025/11/4/周二  13:47
 * @Version: 1.0
 * *****************************************
 */
class ItemEpisodeListVm(
    private val eventVm: ItemEventsViewModel<EpisodeListEvent>,
    listingType: Int,
    myTvSeriesListInfo: MyTvSeriesListInfo?
) : AppItemViewModel<MyTvSeriesListInfo>(myTvSeriesListInfo) {

    fun introduction(): String {
        val introduction = StringBuilder()
        data.get()?.apply {
            introduction.append("共${totalWorkNum}章")
            if (launchStatus == 1) {
                playNum?.let { if (it > 0) introduction.append(" | ${it.formatPlays()}播放") }
                watchingNum?.let { if (it > 0) introduction.append(" | ${it.formatPlays()}追更") }
            } else {
                introduction.append(" | ")
            }
        }
        return introduction.toString()
    }

    fun transactionDate(): String? {
        return data.get()?.let {
            if (it.launchStatus == 1)
                DateUtil.format(it.launchTime ?: 0L, DateUtil.FORMAT_DATETIME_BIAS) + " 上架"
            else
                DateUtil.format(it.createTime ?: 0L, DateUtil.FORMAT_DATETIME_BIAS) + " 创建"
        }
    }

    fun onItemClick() {
        eventVm.postEvent(EpisodeListEvent.ItemClick(data.get()))
    }

    fun launch() {
        eventVm.postEvent(EpisodeListEvent.Launch(data.get()))
    }

    fun share() {
        eventVm.postEvent(EpisodeListEvent.Share(data.get()))
    }
}