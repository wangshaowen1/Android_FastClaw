package com.kgmedia.fastfilm.feature.message.systemnotice

import android.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.network.apis.entity.SystemNoticeInfo
import com.kgmedia.fastfilm.feature.message.MessagesRepository
import kotlinx.coroutines.flow.Flow

/**
 * 系统通知
 */
class SystemNoticeViewModel : AppRepositoryViewModel<MessagesRepository>() {
    var toolbar = ToolbarViewModel(navigationAction = { back() }, title = "系统通知", background = Color.TRANSPARENT)

    fun systemNoticeFlow(): Flow<PagingData<SystemNoticeInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = { SystemNoticePagingSource() }
        ).flow.cachedIn(viewModelScope)
    }

}