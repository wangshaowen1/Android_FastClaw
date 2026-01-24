package com.kgmedia.fastfilm.feature.editor.history

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.network.apis.entity.SegmentImageHistoryInfo
import com.kgmedia.fastfilm.feature.PhotoPagerActivity

/**
 * 分镜历史
 */
class ItemStoryboardHistoryVM(val index: Int, data: SegmentImageHistoryInfo?) : AppItemViewModel<SegmentImageHistoryInfo>(data) {
    val selected by lazy { ObservableBoolean(false) }
    fun SegmentImageHistoryInfo?.genStatusText() = when (this?.genStatus) {
        0 -> "画面排队中\n请稍等"
        1 -> "画面生成中\n请稍等"
        else -> null
    }

    fun preview() {
        topActivity?.let { PhotoPagerActivity.launch(it, data.get()?.shotSegmentImageUrl) }
    }
}