package com.kgmedia.fastfilm.feature.creator.continuewriting

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.data.CommonRepository
import com.kgmedia.fastfilm.core.network.apis.entity.MyTvSeriesListInfo
import com.kgmedia.fastfilm.feature.creator.data.ContinueWritingPagingSource
import kotlinx.coroutines.flow.Flow


class ContinueWritingListViewModel : AppRepositoryViewModel<CommonRepository>() {


    val dataGet: Flow<PagingData<MyTvSeriesListInfo>> = Pager(
        config = PagingConfig(initialLoadSize = 20, pageSize = 20),
        pagingSourceFactory = { ContinueWritingPagingSource() }
    ).flow.cachedIn(viewModelScope)
}