package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.text.Editable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.utils.KeyboardUtils
import com.kgmedia.fastfilm.common.utils.SensitiveHighlighter
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep2Binding
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast


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
    var dialog: GeneratingDialog? = null

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onAppTrack(AppEvent.SF_VIEW_SCRIPT_PAGE)
        activity?.intent?.let {
            mRealVM.workId.value = hostVm?.workId
            //当前正在创作的剧集id
            mRealVM.episodeId = hostVm?.storyId
            //上一集的剧集id,只有续写的时候才有
            mRealVM.previousStoryId = hostVm?.previousStoryId
            mRealVM.inspiration = it.getStringExtra(Constants.Extra.CONTENT)
            mRealVM.model = it.getIntExtra(Constants.Extra.MODE, 0)
            mRealVM.customEpisodeCount = it.getStringExtra(StoryEditorActivity.CUSTOM_EPISODE_COUNT)
            mRealVM.episodeRangeCode = it.getStringExtra(StoryEditorActivity.EPISODE_RANGE_CODE)
            mRealVM.videoStyleId = it.getStringExtra(Constants.Extra.STYLE_ID)
            mRealVM.videoRatio = it.getStringExtra(Constants.Extra.VIDEO_RATIO)
            mRealVM.chapterIndex = it.getIntExtra(Constants.Extra.CHAPTER_INDEX, 1)
            mRealVM.sseGenerate()
            hostVm?.backWarning = true
        }
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })
        binding.btnRegenerate.setOnSingleClickListener {
            if (mRealVM.isStreaming()) return@setOnSingleClickListener
            hostVm?.backWarning = true
            mRealVM.sseGenerate()
        }
        binding.retryBtn.setOnSingleClickListener {
            if (mRealVM.isStreaming()) return@setOnSingleClickListener
            hostVm?.backWarning = true
            mRealVM.sseGenerate()
        }
        binding.btnNext.setOnSingleClickListener {
            if (mRealVM.isStreaming()) return@setOnSingleClickListener
            //检查敏感词
            mRealVM.sensitiveWordJudgment()
        }
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (mRealVM.isStreaming()) return super.onSingleTapConfirmed(e)
                mActivity.showConfirmPrompt(
                    "您想要编辑文字内容，修改内容可能影响 AI 视频生成质量.",
                    confirmText = "继续编辑"
                ) {
                    enterEditMode()
                }
                return true
            }
        })
        binding.inputView.setOnTouchListener({ v, event ->
            val consumed = gestureDetector.onTouchEvent(event)
            consumed
        })

        binding.btnDelete.setOnSingleClickListener {
            if (mRealVM.isStreaming()) return@setOnSingleClickListener
            mRealVM.input.value = ""
        }
    }

    private fun enterEditMode() {
        binding.inputView.let { editText ->
            editText.setOnTouchListener(null)
            editText.setFocusable(true)
            editText.setFocusableInTouchMode(true)
            editText.setCursorVisible(true)

            editText.requestFocus()
            editText.postDelayed({
                KeyboardUtils.showKeyboard(editText)
            }, 100)
        }
    }

    private fun dataUpdate() {
        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = activity?.let { it1 -> GeneratingDialog(it1, it.second) }
                dialog?.show()
            } else {
                dialog?.dismiss()
            }
        }

        mRealVM.input.observe(this) {
            val text = binding.inputView.text.toString()
            if (mRealVM.input.value != text) {
                binding.inputView.setText(mRealVM.input.value)
            }
        }

        mRealVM.sseOutputComplete.observe(this) {
            hostVm?.backWarning = false
        }

        mRealVM.mWorkName.observe(this) {
            if (hostVm?.title?.value.isNullOrEmpty()) {
                hostVm?.title?.value = it
            }
        }
        hostVm?.title?.observe(this) {
            if (it != mRealVM.mWorkName.value) {
                mRealVM.mWorkName.value = it
            }
        }

        mRealVM.sensitiveWordJudgmentResult.observe(this) {
            if (it == null) return@observe
            mRealVM.sensitiveWordJudgmentResult.value = null
            if (it.isSensitive()) {
                it.content?.showToast()
                val sp = SensitiveHighlighter.highlight(
                    text = mRealVM.input.value,
                    keywords = it.hitWords,
                    color = R.color.func_failure_dark.getResColor()
                )
                binding.inputView.setText(sp)
                return@observe
            }
            //敏感词没问题，开始下一步
            mRealVM.confirmGenerate(hostVm?.title?.value) { workId, storyId, taskId ->
                hostVm?.nextStep(3, workId, storyId, taskId)
            }
        }
    }
}