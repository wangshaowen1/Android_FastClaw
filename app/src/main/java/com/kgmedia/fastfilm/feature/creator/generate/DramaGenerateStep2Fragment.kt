package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.text.Editable
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep2Binding
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 *  故事剧本生成
 */
class DramaGenerateStep2Fragment :
    BaseViewModelFragment<FragmentDramaGenerateStep2Binding, DramaGenerateStep2ViewModel>() {

    companion object {
        fun newInstance(): DramaGenerateStep2Fragment {
            val fragment = DramaGenerateStep2Fragment()
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
            mRealVM.workId.value = hostVm?.workId
            mRealVM.episodeId = hostVm?.episodeId
            mRealVM.inspiration = it.getStringExtra(Constants.Extra.CONTENT)
            mRealVM.model = it.getIntExtra(Constants.Extra.MODE, 0)
            mRealVM.customEpisodeCount = it.getStringExtra(StoryEditorActivity.CUSTOM_EPISODE_COUNT)
            mRealVM.episodeRangeCode = it.getStringExtra(StoryEditorActivity.EPISODE_RANGE_CODE)
            mRealVM.videoStyleId = it.getStringExtra(Constants.Extra.STYLE_ID)
            mRealVM.videoRatio = it.getStringExtra(Constants.Extra.VIDEO_RATIO)
            mRealVM.sseGenerate()
            hostVm?.backWarning = true
        }
        mRealVM.input.observe(this) {
            if (mRealVM.input.value != binding.inputView.text) {
                binding.inputView.setText(mRealVM.input.value)
            }
        }
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })
        mRealVM.workId.observe(this) {
            hostVm?.backWarning = false
            hostVm?.workId = it
        }

        binding.btnRegenerate.setOnSingleClickListener {
            mRealVM.sseGenerate()
        }
        binding.btnDelete.setOnClickListener {
            mRealVM.input.value = ""
        }
        binding.btnNext.setOnSingleClickListener {
            mRealVM.confirmGenerate { workId, episodeId ->
                hostVm?.switch2Step(3, workId, episodeId)
            }
        }
    }
}