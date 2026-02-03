package com.kgmedia.fastfilm.feature.series

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.PagedProxy
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import kotlinx.coroutines.flow.Flow

/**
 * 剧集详情页
 */
class SeriesViewModel : AppRepositoryViewModel<SeriesRepository>() {
    private val workId by lazy { mBundle.getString(Constants.Extra.ID) }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val workInfo by lazy { ObservableField<WorkInfo>() }
    override fun onCreate() {
        super.onCreate()
        getWorkDtl()
    }

    fun getWorkDtl() {
        loadState.set(LoadStateViewHolder.LOADING)
        if (workInfo.get() == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                mRepo.getWorkInfo(workId).data?.let {
                    workInfo.set(it)
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
        if (workInfo.get() == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    fun episodeList(): Flow<PagingData<EpisodeInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = PagedProxy.DEFAULT_PAGE_SIZE * 3,
                pageSize = PagedProxy.DEFAULT_PAGE_SIZE
            ),
            pagingSourceFactory = {
                EpisodePagingSource(workId)
            }
        ).flow.cachedIn(viewModelScope)
    }
}