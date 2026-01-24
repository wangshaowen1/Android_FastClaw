package com.kgmedia.fastfilm.feature.timbre

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.UpdateTimbreTitleBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

class CommonEditorDialogViewModel : AppRepositoryViewModel<AppRepository>() {

    val id by lazy { mBundle.getString(Constants.Extra.ID) }

    //临时修改的
    val content = MutableLiveData("")
    fun clearContent(view: View) {
        content.value = ""
    }


    /**
     * 修改歌曲信息
     */
    fun modifyTimbreName(
        callback: (() -> Unit)?
    ) {
        launchWithLoading {
            response(mRepo.api.updateTimbreTitle(UpdateTimbreTitleBody(content.value, id))) {
                R.string.song_title_modified_successfully.getResString().showToast()
                callback?.invoke()
                dismiss()
            }
        }
    }
}