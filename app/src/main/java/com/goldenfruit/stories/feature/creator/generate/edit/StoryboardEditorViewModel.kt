package com.kgmedia.fastfilm.feature.creator.generate.edit

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.SceneContentEditBody
import com.kgmedia.fastfilm.core.network.apis.response.ShotSegmentInfo
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.getParcelableValue

class StoryboardEditorViewModel : AppRepositoryViewModel<CreationRepository>() {

    val maxInputLength = ObservableInt(500)
    val dramaId by lazy { mBundle.getString(Constants.Extra.ID) }
    val segmentInfo by lazy { mBundle.getParcelableValue(Constants.Extra.CONTENT, ShotSegmentInfo::class.java) }
    var title = ObservableField<String>()
    var hint = ObservableField<String>()

    val input = MutableLiveData<String>()

    override fun onCreate() {
        super.onCreate()
        val index = segmentInfo?.sequenceNumber
        val shortDramas = Config.get().appConfig.shortDramas
        maxInputLength.set(shortDramas?.shortDramasSceneDescriptionLimitNum ?: 500)
        title.set("分镜${index}")
        hint.set("请输入画面内容...")
        input.value = segmentInfo?.sceneContent ?: ""
    }

    fun modifyContent(data: Pair<String, List<String>>, onSuccess: (String) -> Unit) {
        launchWithLoading {
            response(
                mRepo.sceneContentEdit(
                    SceneContentEditBody(
                        dramaId,
                        segmentInfo?.shotSegmentId,
                        data.first
                    )
                )
            ) {
                onSuccess.invoke(data.first)
            }
        }
    }
}