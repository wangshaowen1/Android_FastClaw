package com.kgmedia.fastfilm.feature.invitation.income

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.response.InviteIncomePageInfo
import kotlinx.coroutines.flow.Flow

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请收益列表
 * @Data: 2025/10/13/周一  14:13
 * @Version: 1.0
 * *****************************************
 */
class InvitationIncomeViewModel: AppRepositoryViewModel<AppRepository>() {


    fun invitationIncome(): Flow<PagingData<InviteIncomePageInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                InvitationIncomePagingSource()
            }).flow.cachedIn(viewModelScope)
    }
}