package com.kgmedia.fastfilm.feature.creator.storyboard

import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.databinding.FragmentStoryboardDesignBinding
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorViewModel
import com.kgmedia.fastfilm.feature.editor.EditStoryboardVideoActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getParentViewModel

/**
 * 分镜设计
 */
class StoryboardDesignFragment : BaseViewModelFragment<FragmentStoryboardDesignBinding, StoryboardDesignViewModel>() {

    companion object {
        fun newInstance() = StoryboardDesignFragment()
    }

    val hostVm by lazy { getParentViewModel<StoryEditorViewModel>() }

    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.workId = hostVm?.workId
        mRealVM.storyId = hostVm?.storyId
        mRealVM.retry()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnNext -> {
                mRealVM.confirmGenerate { workId, storyId, taskId ->
                    EditStoryboardVideoActivity.start(workId, storyId)
                    activity?.finish()
                }
            }
        }
    }

}