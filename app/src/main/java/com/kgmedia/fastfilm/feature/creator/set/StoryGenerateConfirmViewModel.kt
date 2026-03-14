package com.kgmedia.fastfilm.feature.creator.set

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast


class StoryGenerateConfirmViewModel : AppRepositoryViewModel<AppRepository>() {
    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(true) }
    val body by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, UploadStoryBody::class.java) }
    fun confirmPowerConsume(callback: (UploadStoryData?) -> Unit) {
        if (body == null) return
        requestWithLoading(
            apiBlock = {
                mRepo.api.novelUploadSummary(body!!)
            },
            onSuccess = { callback.invoke(data) },
            onError = {
                if (it is ApiException) {
                    if (it.errorCode == ApiResponse.RESPONSE_CODE_POWER_NOT_ENOUGH) {
                        PowerRechargeDialog.show()
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