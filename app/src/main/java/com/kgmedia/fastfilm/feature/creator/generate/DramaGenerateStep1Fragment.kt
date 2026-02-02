package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fluid.afm.markdown.widget.PrinterMarkDownTextView
import com.fluid.afm.styles.MarkdownStyles
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep1Binding
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 故事梗概生成
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
    val markDownTextView by lazy { PrinterMarkDownTextView(mActivity) }

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

        val linearLayoutManager = LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.setLayoutManager(linearLayoutManager)

        linearLayoutManager.setStackFromEnd(true)
        binding.recyclerView.setAdapter(object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(markDownTextView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            }

            override fun getItemCount(): Int {
                return 1
            }
        })
        markDownTextView.init(MarkdownStyles.getDefaultStyles(), null)
        markDownTextView.setPrintParams(25, 1)

        mRealVM.mSSEContent.observe(this) {
            markDownTextView.startPrinting(it)
            //流式使用方式 或许性能会更好 暂时不改了 markDownTextView.appendPrinting(it)
        }
        mRealVM.onSSECompleted = {
            markDownTextView.stopPrinting("")
        }
        mRealVM.workId.observe(this) {
            hostVm?.backWarning = false
            hostVm?.workId = it
        }
        mRealVM.mWorkName.observe(this) {
            if (hostVm?.title?.value != it) {
                hostVm?.title?.value = it
            }
        }
        hostVm?.title?.observe(this) {
            if (it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
            }
        }
        binding.btnNext.setOnSingleClickListener {
            mRealVM.confirmGenerate { workId ->
                hostVm?.switch2Step(2, workId)
            }
        }
    }

    override fun onDestroy() {
        if (markDownTextView.isStarted) {
            markDownTextView.stopPrinting("")
        }
        super.onDestroy()
    }
}