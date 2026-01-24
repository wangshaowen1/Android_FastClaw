package com.kgmedia.fastfilm.feature.wallet.details

import android.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.response.WithdrawalRecordInfo
import kotlinx.coroutines.flow.Flow

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:提现明细
 * @Data: 2025/10/14/周二  10:00
 * @Version: 1.0
 * *****************************************
 */
class WithdrawalDetailsViewModel : AppRepositoryViewModel<AppRepository>() {
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "提现明细",
        background = Color.TRANSPARENT,
    )

    fun withdrawalDetails(): Flow<PagingData<WithdrawalRecordInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                WithdrawalDetailsPagingSource()
            }).flow.cachedIn(viewModelScope)
    }
}