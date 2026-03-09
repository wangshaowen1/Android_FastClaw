package com.kgmedia.fastfilm.feature.series

import android.annotation.SuppressLint
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.feature.works.ItemMultipleChoiceEventsViewModel
import kotlinx.coroutines.launch

/**
 * 剧集-item
 */
class ItemSeriesEpisodeVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<SeriesEpisodeEvent, String>,
    val position: Int,
    private val episodeInfo: EpisodeInfo?,
) : AppItemViewModel<EpisodeInfo>(episodeInfo) {

    //当前显示状态是查看(true)还是编辑(false)
    var isView = ObservableBoolean(true)

    //是否选中
    val isSelected = ObservableBoolean(false)

    fun onClick() {
        if (isView.get()) {
            eventVm.postEvent(SeriesEpisodeEvent.Click(position, this))
            return
        }
        if (isSelected.get()) {
            isSelected.set(false)
            eventVm.removeSelectedItem(episodeInfo?.storyId ?: "")
        } else {
            isSelected.set(true)
            eventVm.addSelectedItem(episodeInfo?.storyId ?: "")
        }
        eventVm.postEvent(SeriesEpisodeEvent.ComposeState(isSelected.get(), this))
    }

    init {
        viewModelScope.launch {
            eventVm.switchStatus.collect {
                isView.set(it)
            }
        }

        viewModelScope.launch {
            eventVm.selectedItemIds.collect { selectedIds ->
                isSelected.set(selectedIds.contains(episodeInfo?.storyId))
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatDuration(): String {
        val millis=data.get()?.duration?:0L
        if (millis <= 0L) return "0"
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}