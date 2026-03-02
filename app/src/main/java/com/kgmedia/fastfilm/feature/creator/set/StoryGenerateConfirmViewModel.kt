package com.kgmedia.fastfilm.feature.creator.set

import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.utils.FileCountUtils
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity


class StoryGenerateConfirmViewModel : AppRepositoryViewModel<AppRepository>() {

    val title: String? = mBundle.getString(Constants.Extra.TITLE)
    val videoRatio: String? = mBundle.getString(Constants.Extra.VIDEO_RATIO)
    val styleId: String? = mBundle.getString(Constants.Extra.STYLE_ID)
    val episodeRangeCode: String? = mBundle.getString(StoryEditorActivity.EPISODE_RANGE_CODE)
    val customEpisodeCount: String? = mBundle.getString(StoryEditorActivity.CUSTOM_EPISODE_COUNT)
    fun confirmPowerConsume(fileUrl: String?, result: FileCountUtils.AnalysisResult?, callback: (UploadStoryData?) -> Unit) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.novelUploadSummary(
                    UploadStoryBody(
                        result?.charCount,
                        fileUrl,
                        title,
                        videoRatio,
                        styleId,
                        episodeRangeCode,
                        customEpisodeCount
                    )
                )
            }
        ) {
            callback.invoke(data)
        }
    }
}