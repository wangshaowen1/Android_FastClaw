package com.kgmedia.fastfilm.feature.play

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableLong
import androidx.media3.common.Player
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.entity.ProfitDetailInfo
import com.kgmedia.fastfilm.core.network.apis.entity.TrackInfo
import com.kgmedia.fastfilm.widget.PlayerSeekbar
import com.nhsd.basic.utils.Logger


class ItemVideoFeedVm(info: TrackInfo?) :
    AppItemViewModel<TrackInfo>(info) {

    companion object{
        const val LOG_TAG = "ItemVideoFeedVm"
    }

    val player = ObservableField<Player>()

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

    val onSeekListener by lazy {
        object : PlayerSeekbar.OnSeekListener {
            override fun onDragEnd(position: Long) {
                Logger.i(LOG_TAG, "onDragEnd seek to position:$position")
                player.get()?.seekTo(position)
                seeking.set(false)
            }

            override fun onDrag() {
                seeking.set(true)
            }
        }
    }
}