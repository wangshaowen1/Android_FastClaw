package com.kgmedia.fastfilm.feature.series

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
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.request.LatestEpisodeBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import kotlinx.coroutines.flow.Flow

/**
 * 剧集详情页
 */
class SeriesViewModel : AppRepositoryViewModel<SeriesRepository>() {
    val workId by lazy { mBundle.getString(Constants.Extra.ID) }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val workInfo by lazy { MutableLiveData<WorkInfo>() }

    //创建续集信息
    val episodeInfo = MutableLiveData<EpisodeInfo?>()

    override fun onCreate() {
        super.onCreate()
        getWorkDtl()
    }

    /**
     *作品详情
     */
    fun getWorkDtl() {
        loadState.set(LoadStateViewHolder.LOADING)
        if (workInfo.value == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                mRepo.getWorkInfo(workId).data?.let {
                    workInfo.value = it
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
        if (workInfo.value == null) {
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
            pagingSourceFactory = { EpisodePagingSource(workId, workInfo.value?.totalEpisodeCnt ?: 0) }
        ).flow.cachedIn(viewModelScope)
    }

    /**
     *获取剧集最新一集剧本详情
     */
    fun getEpisodeInfo() {
        launchWithLoading {
            response(
                mRepo.api.latestEpisode(
                    LatestEpisodeBody(
                        workType = LatestEpisodeBody.WORK_TYPE_INSPIRATION,
                        workId = workId
                    )
                )
            ) {
                episodeInfo.value = data
            }
        }
    }
}