package com.kgmedia.fastfilm.feature.creator.set

import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.RecyclerViewVM
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigData
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigInfo
import com.kgmedia.fastfilm.core.network.apis.entity.VideoStyleConfigInfo
import com.kgmedia.fastfilm.feature.creator.style.ItemCreationStyleVm
import com.kgmedia.fastfilm.widget.decoration.GridSpacingItemDecoration
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParcelableValue

/**
 * *****************************************
 * @Introduce:创作
 * *****************************************
 */
class VideoSetViewModel : AppRepositoryViewModel<AppRepository>() {
    val configsData by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, VideoConfigData::class.java) }

    var checkedRatio: ItemVideoConfigVm? = null
    var checkedEpisodeSet: ItemVideoConfigVm? = null
    var checkedStyle: ItemCreationStyleVm? = null

    val ratioVms = arrayListOf<ItemVideoConfigVm>()
    val styleVms = arrayListOf<ItemCreationStyleVm>()
    val episodeVms = arrayListOf<ItemVideoConfigVm>()

    override fun onCreate() {
        super.onCreate()
        episodeVms.clear()
        ratioVms.clear()
        styleVms.clear()
        configsData?.totalEpisodeCntItemVoList?.forEach {
            episodeVms.add(initEpisodeVm(it))
        }
        configsData?.ratioItemVoList?.forEach {
            ratioVms.add(initRatioVm(it))
        }
        configsData?.list?.forEach {
            styleVms.add(initStyleVm(it))
        }
    }

    fun initEpisodeVm(ratio: VideoConfigInfo): ItemVideoConfigVm {
        return ItemVideoConfigVm(ratio).apply {
            if (checkedEpisodeSet == null) {
                checked.set(true)
                checkedEpisodeSet = this
            }
            onItemClick.value = {
                if (checkedEpisodeSet != this) {
                    checkedEpisodeSet?.checked?.set(false)
                    this.checked.set(true)
                    checkedEpisodeSet = this
                }
            }
        }
    }

    fun initRatioVm(ratio: VideoConfigInfo): ItemVideoConfigVm {
        return ItemVideoConfigVm(ratio).apply {
            if (checkedRatio == null) {
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

    fun initStyleVm(info: VideoStyleConfigInfo): ItemCreationStyleVm {
        return ItemCreationStyleVm(info).apply {
            if (checkedStyle == null) {
                checked.set(true)
                checkedStyle = this
            }
            onItemClick.value = {
                if (checkedStyle != this) {
                    checkedStyle?.checked?.set(false)
                    this.checked.set(true)
                    checkedStyle = this
                }
            }
        }
    }

    val mRatioAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_video_ratio, ratioVms)
            .setItemDecoration(
                GridSpacingItemDecoration(
                    2,
                    12.dp,
                    0
                )
            ).build()

    val mStyleAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_creation_style, styleVms).build()

    val mEpisodeAdapterVm =
        RecyclerViewVM().Builder().setQuickAdapter(R.layout.item_series_count, episodeVms).build()
}