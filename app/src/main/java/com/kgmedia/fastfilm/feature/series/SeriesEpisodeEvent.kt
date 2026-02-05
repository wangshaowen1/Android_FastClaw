package com.kgmedia.fastfilm.feature.series

sealed class SeriesEpisodeEvent {
    class Click(val position: Int, val item: ItemSeriesEpisodeVM) : SeriesEpisodeEvent()

    //编制状态下 status是已经改变后的状态
    class ComposeState(status: Boolean,val item: ItemSeriesEpisodeVM?):SeriesEpisodeEvent()
}