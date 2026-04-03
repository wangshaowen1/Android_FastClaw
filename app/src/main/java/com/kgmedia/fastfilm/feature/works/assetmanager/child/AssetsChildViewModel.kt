package com.kgmedia.fastfilm.feature.works.assetmanager.child

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.feature.works.data.AssetChildPagingSource
import kotlinx.coroutines.flow.Flow

class AssetsChildViewModel : AppRepositoryViewModel<AppRepository>() {

    val bizType by lazy { mBundle.getInt(Constants.Extra.TYPE) }
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val storyId by lazy { mBundle.getString(Constants.Extra.STORY_ID) }
    val assetId by lazy { mBundle.getString(Constants.Extra.ASSET_ID) }
    val appearanceId by lazy { mBundle.getString(Constants.Extra.APPEARANCE_ID) }

    val body by lazy {
        QueryAssetBody(assetId = assetId, appearanceId = appearanceId, bizType = bizType, storyId = storyId, workId = workId)
    }

    var totalCountBack: (Int) -> Unit = {}

    fun getAssetsPage(): Flow<PagingData<SimpleAsset>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                AssetChildPagingSource(body, totalCountBack)
            }).flow.cachedIn(viewModelScope)
    }
}