package com.kgmedia.fastfilm.feature.creator.set

import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.utils.FileCountUtils
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData


class StoryGenerateConfirmViewModel : AppRepositoryViewModel<AppRepository>() {
    fun confirmPowerConsume(fileUrl: String?, result: FileCountUtils.AnalysisResult?, callback: (UploadStoryData?) -> Unit) {
        requestWithLoading(
            apiBlock = { mRepo.api.novelUploadSummary(UploadStoryBody(result?.charCount, fileUrl)) }
        ) {
            callback.invoke(data)
        }
    }
}