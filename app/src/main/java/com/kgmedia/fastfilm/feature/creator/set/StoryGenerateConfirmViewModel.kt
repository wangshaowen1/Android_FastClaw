package com.kgmedia.fastfilm.feature.creator.set

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.showRechargeDialog
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkTaskData
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch


class StoryGenerateConfirmViewModel : AppRepositoryViewModel<AppRepository>() {
    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(true) }
    val body by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, UploadStoryBody::class.java) }
    fun confirmPowerConsume(callback: (WorkTaskData?) -> Unit) {
        if (body == null) return
        launchWithLoading {
            val data = mRepo.api.novelUploadSummary(body!!)
            if (data.code == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                viewModelScope.launch {
                    PowerRechargeDialog.show(true)
                }
            } else if (data.isSuccess) {
                callback.invoke(data.data)
            } else {
                data.msg?.showToast()
            }
        }
    }

    fun checkPolicy() {
        checkedPolicy.set(checkedPolicy.get().not())
    }
}