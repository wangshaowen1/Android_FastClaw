package com.kgmedia.fastfilm.feature.creator.style

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.CreationStyleInfo
import com.kgmedia.fastfilm.feature.creator.data.CreationStylePagingSource
import kotlinx.coroutines.flow.Flow

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class CreationStyleChooseViewModel : AppRepositoryViewModel<AppRepository>() {
    val lastStyleId by lazy { mBundle.getString(Constants.Extra.ID) }
    val stylesGet: Flow<PagingData<CreationStyleInfo>> = Pager(
        config = PagingConfig(initialLoadSize = 20, pageSize = 20),
        pagingSourceFactory = { CreationStylePagingSource() }
    ).flow.cachedIn(viewModelScope)
}