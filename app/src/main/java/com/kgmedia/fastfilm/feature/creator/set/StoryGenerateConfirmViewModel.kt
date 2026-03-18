package com.kgmedia.fastfilm.feature.creator.set

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.ext.showRechargeDialog
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkTaskData
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
        requestWithLoading(
            apiBlock = {
                mRepo.api.novelUploadSummary(body!!)
            },
            onSuccess = { callback.invoke(data) },
            onError = {
                if (it is ApiException) {
                    if (it.errorCode == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                        viewModelScope.launch {
                            showRechargeDialog(true)
                        }
                    } else {
                        it.message?.showToast()
                    }
                } else {
                    it?.message?.showToast()
                }
            }
        )
    }

    fun checkPolicy(){
        checkedPolicy.set(checkedPolicy.get().not())
    }
}