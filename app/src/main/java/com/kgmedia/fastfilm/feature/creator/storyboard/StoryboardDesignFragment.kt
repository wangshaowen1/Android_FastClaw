package com.kgmedia.fastfilm.feature.creator.storyboard

import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.databinding.FragmentStoryboardDesignBinding
import com.nhsd.basic.BaseViewModelFragment

/**
 * 分镜设计
 */
class StoryboardDesignFragment : BaseViewModelFragment<FragmentStoryboardDesignBinding, StoryboardDesignViewModel>() {

    companion object {
        fun newInstance(workId: String?, storyId: String?) = StoryboardDesignFragment().also {
            it.arguments = bundleOf(Constants.Extra.WORK_ID to workId, Constants.Extra.STORY_ID to storyId)
        }
    }

}