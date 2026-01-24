package com.kgmedia.fastfilm.feature.mine.episode

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.MyTvSeriesListInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:剧集列表
 * @Data: 2025/11/4/周二  17:50
 * @Version: 1.0
 * *****************************************
 */
class EpisodeListViewModel : AppRepositoryViewModel<AppRepository>() {

    //上架类型
    val listingType by lazy {
        mBundle.getInt(EpisodeListFragment.LISTING_TYPE)
    }

    private val _totalCount = MutableSharedFlow<Int>(replay = 1)
    val totalCount: SharedFlow<Int> = _totalCount.asSharedFlow()

    fun episodeList(): Flow<PagingData<MyTvSeriesListInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                EpisodeListPagingSource(
                    listingType = listingType,
                    onTotalCountUpdate = {
                        _totalCount.tryEmit(it)
                    }
                )
            }).flow.cachedIn(viewModelScope)
    }
}