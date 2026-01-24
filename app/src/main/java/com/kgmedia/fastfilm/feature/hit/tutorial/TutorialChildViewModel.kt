package com.kgmedia.fastfilm.feature.hit.tutorial

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.StrategyInfo
import com.kgmedia.fastfilm.core.network.apis.request.StrategyInfoIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.hit.data.StrategyPagingSource
import kotlinx.coroutines.flow.Flow


class TutorialChildViewModel : AppRepositoryViewModel<AppRepository>() {

    val dataGet: Flow<PagingData<StrategyInfo>> = Pager(
        config = PagingConfig(initialLoadSize = 20, pageSize = 20),
        pagingSourceFactory = { StrategyPagingSource() }
    ).flow.cachedIn(viewModelScope)

    fun clickReport(info: StrategyInfo?) {
        launch {
            response(mRepo.api.tutorialClickReport(StrategyInfoIdBody(info?.strategyInfoId))) {}
        }

    }
}