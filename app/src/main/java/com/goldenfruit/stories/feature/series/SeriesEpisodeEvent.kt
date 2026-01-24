package com.kgmedia.fastfilm.feature.series

sealed class SeriesEpisodeEvent {
    class Click(val position: Int, val item: ItemSeriesEpisodeVM) : SeriesEpisodeEvent()
}