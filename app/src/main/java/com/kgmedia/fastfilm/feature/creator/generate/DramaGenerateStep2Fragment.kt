package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep1Binding
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep2Binding
import com.kgmedia.fastfilm.feature.creator.generate.edit.StoryOutlineEditorDialog
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener

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
            val dramasId = it.getStringExtra(Constants.Extra.ID)
            if (dramasId.isNullOrEmpty()) {
                mRealVM.otherExtraId = it.getStringExtra(Constants.Extra.EXTRA_ID)
                mRealVM.content = it.getStringExtra(Constants.Extra.CONTENT)
                mRealVM.model = it.getIntExtra(Constants.Extra.MODE, DramaGenerateBody.GEN_WAY_INSPIRATION)
                mRealVM.dramaStyleId = it.getStringExtra(Constants.Extra.STYLE_ID)
                mRealVM.videoRatio = it.getIntExtra(Constants.Extra.VIDEO_RATIO, 0)
                mRealVM.sseGenerate()
                hostVm?.backWarning = true
                return@let
            } else {// 有dramasId的情况下走恢复生成（目前只有恢复生成会在步骤1传入dramasId）
                mRealVM.dramaId.value = dramasId
                mRealVM.mContent.set(it.getStringExtra(Constants.Extra.CONTENT))
            }
        }
        // 在文本更新时自动滚动到底部
        binding.tvSSEContent.doAfterTextChanged {
            binding.nsv.postDelayed({
                bindingNullable?.nsv?.fullScroll(View.FOCUS_DOWN)
            }, 100)
        }

        mRealVM.dramaId.observe(this) {
            hostVm?.backWarning = false
            hostVm?.dramaId = it
        }
        mRealVM.mDramaName.observe(this) {
            hostVm?.title?.value = it
        }


        binding.expandTextView.setOnSingleClickListener {
            mRealVM.mContent.get()?.let {
                StoryOutlineEditorDialog.newInstance(mRealVM.dramaId.value, mRealVM.mContent.get()).apply {
                    onSuccess = { content ->
//                        this@DramaGenerateStep1Fragment.mRealVM.refreshDramaContent(content)
                    }
                }.show(childFragmentManager)
            }
        }
    }
}