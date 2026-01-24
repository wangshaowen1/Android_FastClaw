package com.kgmedia.fastfilm.feature.series

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.PagedProxy
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.DramasSeriesInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.bus.FlowEventBus
import kotlinx.coroutines.flow.Flow

/**
 * 剧集详情页
 */
class SeriesViewModel : AppRepositoryViewModel<SeriesRepository>() {
    private val seriesId by lazy { mBundle.getString(Constants.Extra.ID) }
    private val newlyPublish by lazy { mBundle.getBoolean(Constants.Extra.NEWLY_PUBLISH) }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val series by lazy { ObservableField<DramasSeriesInfo>() }
    val subscribed by lazy { ObservableBoolean() }

    /**
     * 0 默认状态 1 已处理 2 需要弹上架弹窗
     */
    val autoSetPrice by lazy { MutableLiveData(0) }
    override fun onCreate() {
        super.onCreate()
        getSeriesInfo()
    }

    fun getSeriesInfo() {
        loadState.set(LoadStateViewHolder.LOADING)
        if (series.get() == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                mRepo.seriesDetail(seriesId).data?.let {
                    series.set(it)
                    subscribed.set(it.followStatus == 1)
                    if (autoSetPrice.value == 0) {
                        autoSetPrice.value = 1
                        if (newlyPublish && it.launchStatus != 1) {
                            autoSetPrice.value = 2
                        }
                    }
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            }
        )
    }

    private fun updateLoadState() {
        loadState.set(LoadStateViewHolder.CONTENT)
        if (series.get() == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    fun subscribe() {
        launchWithLoading {
            response(mRepo.seriesFollow(seriesId, if (subscribed.get()) 0 else 1)) {
                subscribed.set(!subscribed.get())
                FlowEventBus.postSync(Events.SeriesWatchingEvent(seriesId, data.watchingStatus ?: 0, data.watchingNumStr ?: "0"))
            }
        }
    }

    fun episodeList(): Flow<PagingData<ShortDramaInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = PagedProxy.DEFAULT_PAGE_SIZE * 3,
                pageSize = PagedProxy.DEFAULT_PAGE_SIZE
            ),
            pagingSourceFactory = {
                SeriesEpisodePagingSource(seriesId)
            }
        ).flow.cachedIn(viewModelScope)
    }
}