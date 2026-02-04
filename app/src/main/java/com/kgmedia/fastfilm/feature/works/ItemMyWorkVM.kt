package com.kgmedia.fastfilm.feature.works

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import kotlinx.coroutines.launch

class ItemMyWorkVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>,
    private val workInfo: WorkInfo?
) : AppItemViewModel<WorkInfo>(workInfo) {


    //当前显示状态是查看(true)还是编辑(false)
    var isView = ObservableBoolean(true)

    //当前合成状态
    val workState = ObservableField(WorkInfo.COMPLETED_STATE)

    //是否选中
    val isSelected = ObservableBoolean(false)

    fun onClick() {
        if (isView.get()) {
            eventVm.postEvent(MyWorksEvent.ViewDetails(workInfo))
            return
        }
        if (isSelected.get()) {
            isSelected.set(false)
            eventVm.removeSelectedItem(workInfo?.workId?:"")
        } else {
            isSelected.set(true)
            eventVm.addSelectedItem(workInfo?.workId?:"")
        }
        eventVm.postEvent(MyWorksEvent.ComposeState(isSelected.get(), workInfo))
    }

    init {
        viewModelScope.launch {
            eventVm.switchStatus.collect {
                isView.set(it)
            }
        }

        viewModelScope.launch {
            eventVm.selectedItemIds.collect { selectedIds ->
                isSelected.set(selectedIds.contains(workInfo?.workId))
            }
        }
    }
}