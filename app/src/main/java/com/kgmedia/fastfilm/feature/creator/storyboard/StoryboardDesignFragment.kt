package com.kgmedia.fastfilm.feature.creator.storyboard

import android.view.View
import androidx.databinding.Observable
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.applySmartFocusManager
import com.kgmedia.fastfilm.common.ext.showConfirmDialog
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentStoryboardDesignBinding
import com.kgmedia.fastfilm.feature.creator.generate.OnBackPressedHandler
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorViewModel
import com.kgmedia.fastfilm.feature.editor.EditStoryboardVideoActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getParentViewModel

/**
 * 分镜设计
 */
class StoryboardDesignFragment :
    BaseViewModelFragment<FragmentStoryboardDesignBinding, StoryboardDesignViewModel>(), OnBackPressedHandler {

    companion object {
        fun newInstance() = StoryboardDesignFragment()
    }

    private val hostVm by lazy { getParentViewModel<StoryEditorViewModel>() }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        mRealVM.workId = hostVm?.workId
        mRealVM.storyId = hostVm?.storyId
        mRealVM.taskId = hostVm?.taskId
        mRealVM.step.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                Global.refreshBalance()
            }
        })
        mRealVM.retry()
        EventManager.onAppTrack(AppEvent.SF_VIEW_STORYBOARD_DESIGN_PAGE)
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnNext -> {
                mRealVM.clickNext { workId, storyId, taskId ->
                    EditStoryboardVideoActivity.start(
                        hostVm?.title?.value?.let { "$it：第${hostVm?.chapterIndex ?: 1}集" },
                        workId,
                        storyId,
                        hostVm?.videoRatio,
                        null
                    )
                    activity?.finish()
                }
            }
        }
    }

    override fun handleBackPressed(): Boolean {
        activity?.showConfirmDialog(
            title = null,
            tips = "退出后，当前内容将继续生成，不会影响您的创作进度。您可随时返回继续创作。",
            confirm = {
                activity?.finish()
            }
        )
        return true
    }

}