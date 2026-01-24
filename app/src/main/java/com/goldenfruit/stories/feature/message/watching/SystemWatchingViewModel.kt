package com.kgmedia.fastfilm.feature.message.watching

import android.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageWatchingInfo
import com.kgmedia.fastfilm.feature.message.MessagesRepository
import kotlinx.coroutines.flow.Flow

class SystemWatchingViewModel : AppRepositoryViewModel<MessagesRepository>() {
    var toolbar = ToolbarViewModel(navigationAction = { back() }, title = "追更", background = Color.TRANSPARENT)

    fun msgFlow(): Flow<PagingData<SystemMessageWatchingInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = { SystemWatchingPagingSource() }
        ).flow.cachedIn(viewModelScope)
    }

}