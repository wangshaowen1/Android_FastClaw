package com.kgmedia.fastfilm.feature.payment.points.details

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.PointConsumePageInfo
import com.kgmedia.fastfilm.core.network.apis.response.RechargePageInfo
import kotlinx.coroutines.flow.Flow

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:余额明细
 * @Data: 2025/9/10/周三  17:50
 * @Version: 1.0
 * *****************************************
 */
class ComputingPowerDetailsFragmentViewModel : AppRepositoryViewModel<AppRepository>() {


    /**
     * 获取积分消耗明细分页
     * @return Flow<PagingData<ConsumptionDetailsData>>
     */
    fun rechargeDetails(): Flow<PagingData<RechargePageInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                RechargePagePagingSource()

            }).flow.cachedIn(viewModelScope)
    }

    /**
     * 获取积分消耗明细分页
     * @return Flow<PagingData<RechargePageInfo>>
     */
    fun getPointConsumePage(): Flow<PagingData<PointConsumePageInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                PointConsumePagePagingSource()
            }).flow.cachedIn(viewModelScope)
    }
}