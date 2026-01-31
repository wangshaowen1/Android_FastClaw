package com.kgmedia.fastfilm.feature.creator.generate.edit

import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.request
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.core.network.apis.request.ReferenceImageBody
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.ext.dp

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class ReferenceHistoricalImageryViewModel : AppRepositoryViewModel<AppRepository>() {
    var checkedVm: ItemReferenceImageVm? = null

    val imagesVms = arrayListOf<ItemReferenceImageVm>()

    fun initVm(info: ReferenceImageInfo): ItemReferenceImageVm {
        return ItemReferenceImageVm(info).apply {
            if (checkedVm == null) {
                checked.set(true)
                checkedVm = this
            }
            onItemClick.value = {
                if (checkedVm != this) {
                    checkedVm?.checked?.set(false)
                    this.checked.set(true)
                    checkedVm = this
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        getReferenceHistoricalImagery()
    }


    fun getReferenceHistoricalImagery() {
        //todo 入参填入
        request({ mRepo.api.getHistoryReferenceImage(ReferenceImageBody()) }) {
            data?.forEach {
                imagesVms.add(initVm(it))
            }
            mAdapterVm.notifyDataSetChanged()
        }
    }


    val mAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_reference_historical_imagery, imagesVms).build()
}