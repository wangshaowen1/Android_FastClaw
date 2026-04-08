package com.kgmedia.fastfilm.feature.works.assetmanager.roleimage

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.RoleImageInfo
import com.nhsd.basic.item.BaseItemEventsViewModel
import kotlinx.coroutines.launch

class ItemRoleImageVm(
    data: RoleImageInfo?,
    val model: Boolean = false,
    val itemClickVm: BaseItemEventsViewModel<RoleImageEvent, RoleImageInfo>
) : AppItemViewModel<RoleImageInfo>(data) {
    val selectModel = ObservableBoolean(false)
    val checked = ObservableBoolean(false)

    init {
        selectModel.set(model)
        if (model) {
            viewModelScope.launch {
                itemClickVm.events.collect {
                    when (it) {
                        is RoleImageEvent.Click -> {
                        }

                        is RoleImageEvent.Select -> {
                            checked.set(it.item?.data?.get()?.appearanceId == data?.appearanceId)
                        }
                    }
                }
            }
        }
    }

    fun onItemClick(v: View?) {
        if (model) {
            itemClickVm.postEvent(RoleImageEvent.Select(this))
        } else {
            itemClickVm.postEvent(RoleImageEvent.Click(this))
        }
    }
}