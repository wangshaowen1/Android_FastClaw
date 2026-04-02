package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep1Binding
import com.kgmedia.fastfilm.feature.creator.CreationRootActivity
import com.kgmedia.fastfilm.feature.creator.StoryReuploadDialog
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kgmedia.fastfilm.feature.editor.info.InputDialog
import com.kgmedia.fastfilm.feature.works.MyWorksFragment
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast


/**
 * 故事梗概生成
 */
class DramaGenerateStep1Fragment :
    BaseViewModelFragment<FragmentDramaGenerateStep1Binding, DramaGenerateStep1ViewModel>(), OnBackPressedHandler {

    companion object {
        const val TAG = "DramaGenerateStep1Fragment"
        fun newInstance(): DramaGenerateStep1Fragment {
            val fragment = DramaGenerateStep1Fragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }
    var episodeAdapter = QuickAdapter<ItemStorySummaryGenerateVm>(R.layout.item_story_summary_generate, emptyList())
    private var showBackToast = false
    val from by lazy { hostVm?.from }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onAppTrack(AppEvent.SF_VIEW_STORY_OUTLINE_PAGE)
        activity?.intent?.let {
            mRealVM.workId = it.getStringExtra(Constants.Extra.WORK_ID)
            mRealVM.model = it.getIntExtra(Constants.Extra.MODE, 0)
            mRealVM.refreshDisplayContent()
            mRealVM.pollSummary()
            showBackToast = true
        }

        binding.recyclerView.setAdapter(episodeAdapter)
        mRealVM.storySummaryResult.observe(this) {
            if (it == null) return@observe
            showBackToast = false
            if (it.isSuccess()) {
                setTextWithEditIcon(binding.tvStoryDesc, it.result?.workDesc ?: "")
                episodeAdapter.submitList(it.result?.structureList?.map { info ->
                    return@map ItemStorySummaryGenerateVm(info)
                })
            } else if (it.isSensitive()) {
                StoryReuploadDialog.show(it.novelFileName, it.novelCharCount, it.toastTextAuditFailed) {
                    reuploadStory()
                }
            }
        }

        mRealVM.mWorkName.observe(this) {
            if (hostVm?.title?.value.isNullOrEmpty()) {
                hostVm?.title?.value = it
            }
        }
        hostVm?.title?.observe(this) {
            if (!it.isNullOrEmpty() && it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
                mRealVM.saveTitle()
            }
        }
        binding.btnRegenerate.setOnSingleClickListener {
            if (mRealVM.model == DramaGenerateConstants.MODEL_STORY_DOCUMENTS) {
                reuploadStory()
            } else {
                mRealVM.retrySummary {
                    mRealVM.pollSummary()
                    showBackToast = true
                }
            }
        }
        binding.btnNextDisable.setOnSingleClickListener {
            getString(R.string.story_generating_please_wait).showToast()
        }
        binding.tvStoryDesc.setOnSingleClickListener {
            InputDialog.show(InputDialog.TYPE_WORK_DESC, mRealVM.workId, mRealVM.storySummaryResult.value?.result?.workDesc) {
                mRealVM.storySummaryResult.value?.result?.workDesc = it
                setTextWithEditIcon(binding.tvStoryDesc, it)
            }
        }
        binding.btnNext.setOnSingleClickListener {
            mRealVM.saveTitle {
                hostVm?.nextStep(2, mRealVM.workId, mRealVM.storySummaryResult.value?.result?.getFirstStoryId())
            }
        }
    }


    /**
     * 重新上传/重试  删除本地草稿，打开小说文档上传界面重新上传
     */
    fun reuploadStory() {
        mRealVM.reuploadStory {
            if (!ActivityUtils.isActivityExistsInStack(CreationRootActivity::class.java)) {
                CreationRootActivity.start(mActivity, 1)
            }
            mActivity.finish()
        }
    }

    fun setTextWithEditIcon(textView: TextView, content: String) {
        val editIcon = ContextCompat.getDrawable(textView.context, R.drawable.icon_edit_full)!!

        // 设置图标大小（与文字大小匹配）
        val textSizePx = textView.textSize.toInt()
        editIcon.setBounds(0, 0, textSizePx, textSizePx)

        // 构建 SpannableString
        // 末尾加空格占位 + 图标
        val spaceAndIcon = "  " // 两个空格：一个间距 + 一个给ImageSpan占位
        val fullText = content + spaceAndIcon

        val spannable = SpannableStringBuilder(fullText)

        // 将最后一个字符替换为图片Span
        val imageSpan = ImageSpan(editIcon, ImageSpan.ALIGN_BASELINE)
        spannable.setSpan(
            imageSpan,
            fullText.length - 1,  // 最后一个字符位置
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable
    }


    override fun handleBackPressed(): Boolean {
        if (mRealVM.isSummaryLoading.get()) {
            getString(R.string.ai_generating_auto_saved).showToast()
        } else {
            getString(R.string.auto_saved_as_draft).showToast()
        }
        if (from != StoryEditorActivity.FROM_MY_WORKS) {
            MyWorksFragment.start()
        } else {
            mActivity.finish()
        }
        return true
    }
}