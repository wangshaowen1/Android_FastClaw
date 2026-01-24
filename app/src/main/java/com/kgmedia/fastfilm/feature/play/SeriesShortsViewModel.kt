package com.kgmedia.fastfilm.feature.play

import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.PagedProxy
import com.kgmedia.fastfilm.common.ext.formatPlays
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.DramasSeriesInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.request.DramasSeriesIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.play.data.ShortPlayRepository
import com.kgmedia.fastfilm.feature.play.data.ShortSeriesPagingSource
import com.kgmedia.fastfilm.feature.series.SeriesEpisodePagingSource
import com.kgmedia.fastfilm.kit.PlaylistPlayer
import com.kgmedia.fastfilm.kit.getMediaSourceManager
import com.nhsd.basic.ext.getParcelableValue
import kotlinx.coroutines.flow.Flow


class SeriesShortsViewModel : AppRepositoryViewModel<ShortPlayRepository>() {

    var seriesInfo: DramasSeriesInfo? = null

    val playerInSeries by lazy {
        mBundle.getBoolean(Constants.Extra.PLAYER_IN_SERIES)
    }
    val subTitle = ObservableField<String>()

    var lastTotalWorkNum = 0
    var unTotalWorkNumChange: ((Int) -> Unit)? = null
    var playlistPlayer: PlaylistPlayer? = null
    override fun onCreate() {
        super.onCreate()
        seriesInfo = mBundle.getParcelableValue(Constants.Extra.DATA, DramasSeriesInfo::class.java)
        seriesInfo?.apply {
            lastTotalWorkNum = totalWorkNum ?: 0
            subTitle.set("${playNum?.formatPlays()} 播放·${watchingNum?.formatPlays()} 追更 更新至${totalWorkNum}章")
        }
        loadSeriesDetail()
    }

    fun loadSeriesDetail() {
        launch {
            response(mRepo.api.seriesDetail(DramasSeriesIdBody(seriesInfo?.dramasSeriesId))) {
                data?.apply {
                    seriesInfo = this
                    if (totalWorkNum != lastTotalWorkNum) {
                        unTotalWorkNumChange?.invoke(totalWorkNum ?: 0)
                    }
                    subTitle.set("${playNum?.formatPlays()} 播放·${watchingNum?.formatPlays()} 追更 更新至${totalWorkNum}章")
                }
            }
        }
    }

    fun seriesShortDramas(): Flow<PagingData<ShortDramaInfo>>? {
        return if (playerInSeries) {
            playlistPlayer?.getMediaSourceManager()?.let {
                Pager(
                    config = PagingConfig(
                        initialLoadSize = PagedProxy.DEFAULT_PAGE_SIZE * 3,
                        pageSize = PagedProxy.DEFAULT_PAGE_SIZE
                    ),
                    pagingSourceFactory = {
                        ShortSeriesPagingSource(it)
                    }).flow.cachedIn(viewModelScope)

            }
        } else {
            Pager(
                config = PagingConfig(
                    initialLoadSize = PagedProxy.DEFAULT_PAGE_SIZE * 3,
                    pageSize = PagedProxy.DEFAULT_PAGE_SIZE
                ),
                pagingSourceFactory = {
                    SeriesEpisodePagingSource(seriesInfo?.dramasSeriesId)
                }).flow.cachedIn(viewModelScope)
        }
    }
}