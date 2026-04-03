package com.kgmedia.fastfilm.feature.works.assetmanager

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.feature.works.data.AssetFolderPagingSource
import kotlinx.coroutines.flow.Flow

class AssetFolderViewModel : AppRepositoryViewModel<AppRepository>() {

    fun getAssetFolderPage(): Flow<PagingData<WorkInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                AssetFolderPagingSource()
            }).flow.cachedIn(viewModelScope)
    }
}