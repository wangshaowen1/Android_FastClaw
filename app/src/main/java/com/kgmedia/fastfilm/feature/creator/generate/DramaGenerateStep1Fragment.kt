package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep1Binding
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity

/**
 * 故事更高生成
 */
class DramaGenerateStep1Fragment :
    BaseViewModelFragment<FragmentDramaGenerateStep1Binding, DramaGenerateStep1ViewModel>() {

    companion object {
        fun newInstance(): DramaGenerateStep1Fragment {
            val fragment = DramaGenerateStep1Fragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onAppTrack(AppEvent.JG_VIEW_GENERATE_STORYBOARD_PAGE)
        activity?.intent?.let {
            mRealVM.inspiration = it.getStringExtra(Constants.Extra.CONTENT)
            mRealVM.customEpisodeCount = it.getStringExtra(StoryEditorActivity.CUSTOM_EPISODE_COUNT)
            mRealVM.episodeRangeCode = it.getStringExtra(StoryEditorActivity.EPISODE_RANGE_CODE)
            mRealVM.videoStyleId = it.getStringExtra(Constants.Extra.STYLE_ID)
            mRealVM.videoRatio = it.getStringExtra(Constants.Extra.VIDEO_RATIO)
            mRealVM.sseGenerate()
            hostVm?.backWarning = true
        }

        binding.tvSSEContent.doAfterTextChanged {
            binding.nsv.postDelayed({
                bindingNullable?.nsv?.fullScroll(View.FOCUS_DOWN)
            }, 100)
        }

        mRealVM.workId.observe(this) {
            hostVm?.backWarning = false
            hostVm?.dramaId = it
        }
        mRealVM.mWorkName.observe(this) {
            hostVm?.title?.value = it
        }
    }
}