package com.kgmedia.fastfilm.feature.works

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.response.WorkInformationData
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.launch

class ItemMyWorkVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>,
    private val workInformation: WorkInformationData?
) : AppItemViewModel<WorkInformationData>(workInformation) {


    //当前显示状态是查看(true)还是编辑(false)
    var isView = ObservableBoolean(true)

    //当前合成状态
    val workState = ObservableField(WorkInformationData.COMPLETED_STATE)

    //是否选中
    val isSelected = ObservableBoolean(false)

    fun onClick() {
        if (isView.get()) {
            eventVm.postEvent(MyWorksEvent.ViewDetails(workInformation))
            return
        }
        if (isSelected.get()) {
            isSelected.set(false)
            eventVm.removeSelectedItem(workInformation?.id?:"")
        } else {
            isSelected.set(true)
            eventVm.addSelectedItem(workInformation?.id?:"")
        }
        eventVm.postEvent(MyWorksEvent.ComposeState(isSelected.get(), workInformation))
    }

    init {
        viewModelScope.launch {
            eventVm.switchStatus.collect {
                isView.set(it)
            }
        }

        viewModelScope.launch {
            eventVm.selectedItemIds.collect { selectedIds ->
                isSelected.set(selectedIds.contains(workInformation?.id))
            }
        }
    }
}