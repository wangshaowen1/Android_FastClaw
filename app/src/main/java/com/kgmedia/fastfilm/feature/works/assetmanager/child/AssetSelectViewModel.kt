package com.kgmedia.fastfilm.feature.works.assetmanager.child

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.blankj.utilcode.util.StringUtils.getString
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.feature.works.data.AssetChildPagingSource
import kotlinx.coroutines.flow.Flow

class AssetSelectViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = ""
    )

    val bizType by lazy { mBundle.getInt(Constants.Extra.TYPE) }
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    var selectedItem: SimpleAsset? = null

    var totalCountBack: (Int) -> Unit = {}

    override fun onCreate() {
        super.onCreate()
        when (bizType) {
            QueryAssetBody.BIZ_TYPE_PROP -> mToolbarVM.mTitle.set(getString(R.string.select_prop))
            QueryAssetBody.BIZ_TYPE_SCENE -> mToolbarVM.mTitle.set(getString(R.string.select_scene))
        }
    }

    fun getAssetsPage(): Flow<PagingData<SimpleAsset>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                AssetChildPagingSource(bizType, workId, totalCountBack)
            }).flow.cachedIn(viewModelScope)
    }
}