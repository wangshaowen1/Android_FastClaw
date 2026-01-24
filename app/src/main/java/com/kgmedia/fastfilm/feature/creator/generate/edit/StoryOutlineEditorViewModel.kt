package com.kgmedia.fastfilm.feature.creator.generate.edit

import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository

class StoryOutlineEditorViewModel : AppRepositoryViewModel<CreationRepository>() {

    val maxInputLength = ObservableInt(2000)
    val content by lazy { mBundle.getString(Constants.Extra.CONTENT) }
    val dramaId by lazy { mBundle.getString(Constants.Extra.ID) }
    val input = MutableLiveData<String>()

    override fun onCreate() {
        super.onCreate()
        input.value = content
    }

    fun modifyContent(onSuccess: () -> Unit) {
        launchWithLoading {
            response(mRepo.dramaContentEdit(dramaId, input.value)) {
                onSuccess.invoke()
            }
        }
    }
}