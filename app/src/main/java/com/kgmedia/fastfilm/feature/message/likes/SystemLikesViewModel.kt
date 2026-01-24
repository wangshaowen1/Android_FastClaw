package com.kgmedia.fastfilm.feature.message.likes

import android.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageLikeInfo
import com.kgmedia.fastfilm.feature.message.MessagesRepository
import kotlinx.coroutines.flow.Flow

class SystemLikesViewModel : AppRepositoryViewModel<MessagesRepository>() {
    var toolbar = ToolbarViewModel(navigationAction = { back() }, title = "喜欢", background = Color.TRANSPARENT)

    fun msgFlow(): Flow<PagingData<SystemMessageLikeInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = { SystemLikesPagingSource() }
        ).flow.cachedIn(viewModelScope)
    }

}