package com.kgmedia.fastfilm.feature.editor.info

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.StringUtils.getString
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.SaveStorySummaryBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody

class InputViewModel : AppRepositoryViewModel<AppRepository>() {

    val extra by lazy { mBundle.getString(Constants.Extra.EXTRA) }
    val type by lazy { mBundle.getInt(Constants.Extra.TYPE) }
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val content by lazy { mBundle.getString(Constants.Extra.CONTENT) }
    val storyId by lazy { mBundle.getString(Constants.Extra.STORY_ID) }
    val input = MutableLiveData<String>()
    val title = ObservableField<String>()
    val subTitle = ObservableField<CharSequence>()
    var save = false

    override fun onCreate() {
        super.onCreate()
        input.value = content
    }

    fun setTitle() {
        when (type) {
            InputDialog.TYPE_WORK_DESC -> {
                title.set(getString(R.string.edit_story_summary))
            }

            InputDialog.TYPE_EPISODE_SUMMARY -> {
                title.set(getString(R.string.edit_story_synopsis))
            }
        }
    }

    fun save(onSuccess: (() -> Unit)?) {
        when (type) {
            InputDialog.TYPE_WORK_DESC -> {
                saveDesc { onSuccess?.invoke() }
            }

            InputDialog.TYPE_EPISODE_SUMMARY -> {
                saveStorySummary { onSuccess?.invoke() }
            }
        }
    }

    fun saveDesc(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({
            mRepo.api.updateWorkInfo(
                WorkInfoUpdateBody(
                    workId,
                    workDesc = input.value
                )
            )
        }) {
            onSuccess?.invoke()
        }
    }

    fun saveStorySummary(onSuccess: (() -> Unit)? = null) {
        requestWithLoading({
            mRepo.api.updateStorySummary(
                SaveStorySummaryBody(
                    input.value,
                    storyId,
                    workId
                )
            )
        }) {
            onSuccess?.invoke()
        }
    }
}