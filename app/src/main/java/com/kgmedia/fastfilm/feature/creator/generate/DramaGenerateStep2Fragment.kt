package com.kgmedia.fastfilm.feature.creator.generate

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.common.ext.applySmartFocusManager
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.utils.KeyboardUtils
import com.kgmedia.fastfilm.core.ext.sensitiveWordCheck
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentDramaGenerateStep2Binding
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch

/**
 *  故事剧本生成
 */
class DramaGenerateStep2Fragment :
    BaseViewModelFragment<FragmentDramaGenerateStep2Binding, DramaGenerateStep2ViewModel>(), OnBackPressedHandler {

    companion object {
        fun newInstance(): DramaGenerateStep2Fragment {
            val fragment = DramaGenerateStep2Fragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }
    val from by lazy { hostVm?.from }

    private var showBackToast = false
    private var scrollAnimator: ObjectAnimator? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        EventManager.onAppTrack(AppEvent.SF_VIEW_SCRIPT_PAGE)
        parseIntentData()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun parseIntentData() {
        activity?.intent?.let { intent ->
            mRealVM.workId.value = hostVm?.workId ?: intent.getStringExtra(Constants.Extra.WORK_ID)
            mRealVM.episodeId = hostVm?.storyId ?: intent.getStringExtra(Constants.Extra.STORY_ID)
            mRealVM.previousStoryId = hostVm?.previousStoryId
            mRealVM.model = intent.getIntExtra(Constants.Extra.MODE, 0)
            mRealVM.chapterIndex = intent.getIntExtra(Constants.Extra.CHAPTER_INDEX, 1)

            scriptGenerate()
        }
    }

    private fun scriptGenerate() {
        if (mRealVM.isNovelDocuments()) {
            startPolling()
        } else {
            mRealVM.createScriptTask { startPolling() }
        }
    }

    private fun startPolling() {
        mRealVM.pollNovelScript()
        showBackToast = true
    }

    private fun initView() {
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })
        binding.btnRegenerate.setOnSingleClickListener {
            scriptGenerate()
        }
        binding.retryBtn.setOnSingleClickListener {
            scriptGenerate()
        }
        binding.btnDelete.setOnSingleClickListener {
            if (mRealVM.isScriptLoading.get()) return@setOnSingleClickListener
            mRealVM.input.value = ""
        }

        binding.btnNext.setOnSingleClickListener {
            handleNextStep()
        }
        setupTouchInterceptor()
    }

    private fun handleNextStep() {
        lifecycleScope.launch {
            sensitiveWordCheck(mRealVM.input.value, {
                mRealVM.confirmGenerate { workId, storyId, taskId ->
                    hostVm?.nextStep(3, workId, storyId, taskId)
                }
            }, {
                it?.let {
                    binding.inputView.setText(it)
                }
            })
        }
    }

    private fun setupTouchInterceptor() {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (mRealVM.isScriptLoading.get()) return super.onSingleTapConfirmed(e)

                if (mRealVM.input.value?.trim().isNullOrEmpty()) {
                    enterEditMode()
                    return super.onSingleTapConfirmed(e)
                }

                mActivity.showConfirmPrompt(
                    "您想要编辑文字内容，修改内容可能影响 AI 视频生成质量.",
                    confirmText = "继续编辑"
                ) {
                    enterEditMode()
                }
                return true
            }
        })

        binding.inputView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
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
        mRealVM.input.observe(this) { text ->
            val newText = text ?: ""
            val currentText = binding.inputView.text.toString()

            if (newText != currentText) {
                updateInputViewContent(newText, currentText)
                handleTextScrollAnimation()
            }
        }

        mRealVM.storyScriptResult.observe(this) {
            showBackToast = false
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
    }

    private fun updateInputViewContent(newText: String, currentText: String) {
        // 增量追加优化：避免全量重绘引起的闪烁
        if (newText.startsWith(currentText)) {
            val appendText = newText.substring(currentText.length)
            binding.inputView.append(appendText)
        } else {
            binding.inputView.setText(newText)
        }
    }

    private fun handleTextScrollAnimation() {
        binding.inputView.post {
            val layout = binding.inputView.layout ?: return@post

            val contentHeight = layout.getLineTop(binding.inputView.lineCount)
            val targetScrollY = contentHeight + binding.inputView.paddingTop +
                    binding.inputView.paddingBottom - binding.inputView.height

            if (targetScrollY > 0) {
                // 取消上一次未完成的动画，保持流式输出平滑
                scrollAnimator?.cancel()
                scrollAnimator = ObjectAnimator.ofInt(
                    binding.inputView, "scrollY", binding.inputView.scrollY, targetScrollY
                ).apply {
                    duration = 60
                    interpolator = LinearInterpolator()
                    start()
                }
            } else {
                binding.inputView.scrollTo(0, 0)
            }
        }
    }

    override fun handleBackPressed(): Boolean {
        if (mRealVM.isScriptLoading.get()) {
            "AI正在生成中，已自动保存为草稿".showToast()
        } else {
            "已自动保存为草稿".showToast()
        }
        if (from != StoryEditorActivity.FROM_SERIES) {
            SeriesActivity.start(mRealVM.workId.value)
        }
        mActivity.finish()
        return true
    }
}