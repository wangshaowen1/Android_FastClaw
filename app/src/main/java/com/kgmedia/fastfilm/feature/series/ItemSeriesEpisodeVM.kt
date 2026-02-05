package com.kgmedia.fastfilm.feature.series

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.feature.works.ItemMultipleChoiceEventsViewModel
import com.kgmedia.fastfilm.feature.works.MyWorksEvent
import kotlinx.coroutines.launch

/**
 * 剧集-item
 */
class ItemSeriesEpisodeVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<SeriesEpisodeEvent, String>,
    private val position: Int,
    data: EpisodeInfo?,
    val workInfo: WorkInfo?
) : AppItemViewModel<EpisodeInfo>(data) {

    //当前显示状态是查看(true)还是编辑(false)
    var isView = ObservableBoolean(true)

    //当前合成状态
    val generateStatus = ObservableField(data?.stepProgress)

    //是否选中
    val isSelected = ObservableBoolean(false)

    fun onClick() {

        if (isView.get()) {
            eventVm.postEvent(SeriesEpisodeEvent.Click(position, this))
            return
        }
        if (isSelected.get()) {
            isSelected.set(false)
            eventVm.removeSelectedItem(workInfo?.workId ?: "")
        } else {
            isSelected.set(true)
            eventVm.addSelectedItem(workInfo?.workId ?: "")
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
                isSelected.set(selectedIds.contains(data?.storyId))
            }
        }
    }
}