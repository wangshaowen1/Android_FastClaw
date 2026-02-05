package com.kgmedia.fastfilm.feature.creator.storyboard

import com.kgmedia.fastfilm.databinding.FragmentStoryboardDesignBinding
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorViewModel
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
    }

}