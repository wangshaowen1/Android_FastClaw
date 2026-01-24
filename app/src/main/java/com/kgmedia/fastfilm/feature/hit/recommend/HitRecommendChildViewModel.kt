package com.kgmedia.fastfilm.feature.hit.recommend

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.HitRecommendInfo
import com.kgmedia.fastfilm.core.network.apis.entity.HitTabInfo
import com.kgmedia.fastfilm.feature.hit.data.HitRecommendPagingSource
import com.nhsd.basic.ext.getParcelableValue
import kotlinx.coroutines.flow.Flow


class HitRecommendChildViewModel : AppRepositoryViewModel<AppRepository>() {

    val tabInfo by lazy { mBundle.getParcelableValue(Constants.Extra.TAB, HitTabInfo::class.java) }

    val dataGet: Flow<PagingData<HitRecommendInfo>> = Pager(
        config = PagingConfig(initialLoadSize = 20, pageSize = 20),
        pagingSourceFactory = { HitRecommendPagingSource(tabInfo?.tabId, tabInfo?.tabType) }
    ).flow.cachedIn(viewModelScope)
}