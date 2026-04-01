package com.kgmedia.fastfilm.feature.play

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableLong
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo


class ItemVideoFeedVm(info: WorkInfo?) :
    AppItemViewModel<WorkInfo>(info) {

    companion object {
        const val LOG_TAG = "ItemVideoFeedVm"
    }

    val mPlatformAdapter by lazy { QuickAdapter<ItemPlatformVm>(R.layout.item_platform, emptyList()) }

    init {
        info?.platformLinks?.let { list ->
            mPlatformAdapter.submitList(list.map { ItemPlatformVm(it) })
        }
    }

    /**
     * 是否播放中
     */
    val isPlaying by lazy { ObservableBoolean(false) }

    /**
     * 是否播放中
     */
    val isPaused by lazy { ObservableBoolean(false) }

    /**
     * 当前播放进度ms
     */
    val currentPosition by lazy { ObservableLong(0) }

    /**
     * 总时长ms
     */
    val duration by lazy { ObservableLong(0) }

    /**
     * 拖动进度滑块
     */
    val seeking by lazy { ObservableBoolean(false) }
}