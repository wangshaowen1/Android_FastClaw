package com.kgmedia.fastfilm.feature.creator.generate.edit

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.CreationStyleInfo
import com.kgmedia.fastfilm.core.network.apis.request.CreationGeneratePreBody
import com.kgmedia.fastfilm.feature.creator.ItemVideoRatioVm
import com.kgmedia.fastfilm.feature.creator.data.CreationStylePagingSource
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.flow.Flow

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class ReferenceHistoricalImageryViewModel : AppRepositoryViewModel<AppRepository>() {
    val lastStyleId by lazy { mBundle.getString(Constants.Extra.ID) }

    var checkedRatio: ItemVideoRatioVm? = null

    val ratioVms = arrayListOf(
        initRatioVm(CreationGeneratePreBody.RATIO_9_16),
        initRatioVm(CreationGeneratePreBody.RATIO_16_9),
//        initRatioVm(CreationGeneratePreBody.RATIO_3_4),
//        initRatioVm(CreationGeneratePreBody.RATIO_4_3)
    )

    fun initRatioVm(ratio: Int): ItemVideoRatioVm {
        return ItemVideoRatioVm(ratio).apply {
            if (ratio == CreationGeneratePreBody.RATIO_9_16) {
                checked.set(true)
                checkedRatio = this
            }
            onItemClick.value = {
                if (checkedRatio != this) {
                    checkedRatio?.checked?.set(false)
                    this.checked.set(true)
                    checkedRatio = this
                }
            }
        }
    }
    val stylesGet: Flow<PagingData<CreationStyleInfo>> = Pager(
        config = PagingConfig(initialLoadSize = 20, pageSize = 20),
        pagingSourceFactory = { CreationStylePagingSource() }
    ).flow.cachedIn(viewModelScope)

    val mRatioAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_video_ratio, ratioVms)
            .setItemDecoration(
                GridSpacingItemDecoration(
                    4,
                    8.dp,
                    0
                )
            ).build()
}