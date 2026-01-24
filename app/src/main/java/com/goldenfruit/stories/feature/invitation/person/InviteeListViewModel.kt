package com.kgmedia.fastfilm.feature.invitation.person

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.data.CommonRepository
import com.kgmedia.fastfilm.core.network.apis.response.InviteUserPageInfo
import kotlinx.coroutines.flow.Flow

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请人列表
 * @Data: 2025/10/13/周一  15:24
 * @Version: 1.0
 * *****************************************
 */
class InviteeListViewModel : AppRepositoryViewModel<CommonRepository>() {


    fun inviteeList(): Flow<PagingData<InviteUserPageInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                InviteeListPagingSource()
            }).flow.cachedIn(viewModelScope)
    }
}