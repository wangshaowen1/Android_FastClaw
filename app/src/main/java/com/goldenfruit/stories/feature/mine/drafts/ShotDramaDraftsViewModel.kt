package com.kgmedia.fastfilm.feature.mine.drafts

import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class ShotDramaDraftsViewModel : AppRepositoryViewModel<AppRepository>() {

    private val _totalCount = MutableSharedFlow<Int>(replay = 1)
    val totalCount: SharedFlow<Int> = _totalCount.asSharedFlow()
    // 创建分页数据流
    @OptIn(ExperimentalPagingApi::class)
    fun shortDramas(): Flow<PagingData<ShortDramaInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            remoteMediator = ShortDramaDraftsRemoteMediator(
                database = mRepo.appDatabase,
                onTotalCountUpdate = {
                    Logger.i("total_size0="+it)
                    _totalCount.tryEmit(it)
                }
            )
        ) {
            mRepo.appDatabase.shortDramsDao.getShorts()
        }.flow.cachedIn(viewModelScope)
    }


    fun delete(shortDramaInfo: ShortDramaInfo?) {
        viewModelScope.launch {
            shortDramaInfo?.dramasId?.let {
                mRepo.appDatabase.shortDramsDao
                    .deleteById( it)
            }

        }
    }

    fun update(shortDramaInfo: ShortDramaInfo?) {
        viewModelScope.launch {
            shortDramaInfo?.let { mRepo.appDatabase.shortDramsDao.update(it) }
        }
    }


}