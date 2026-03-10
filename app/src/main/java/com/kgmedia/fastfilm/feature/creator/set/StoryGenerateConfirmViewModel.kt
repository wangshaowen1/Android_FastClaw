package com.kgmedia.fastfilm.feature.creator.set

import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData
import com.nhsd.basic.ext.getParcelableValue


class StoryGenerateConfirmViewModel : AppRepositoryViewModel<AppRepository>() {

    val body by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, UploadStoryBody::class.java) }
    fun confirmPowerConsume(callback: (UploadStoryData?) -> Unit) {
        if (body == null) return
        requestWithLoading(
            apiBlock = {
                mRepo.api.novelUploadSummary(body!!)
            }
        ) {
            callback.invoke(data)
        }
    }
}