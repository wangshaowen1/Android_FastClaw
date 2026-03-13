package com.kgmedia.fastfilm.feature.creator.generate

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.view.GestureDetector
import android.view.MotionEvent
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
import com.kgmedia.fastfilm.widget.GeneratingDialog
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
    var dialog: GeneratingDialog? = null
    private var showBackToast = false

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        EventManager.onAppTrack(AppEvent.SF_VIEW_SCRIPT_PAGE)
        activity?.intent?.let {
            mRealVM.workId.value = hostVm?.workId ?: it.getStringExtra(Constants.Extra.WORK_ID)
            //当前正在创作的剧集id
            mRealVM.episodeId = hostVm?.storyId ?: it.getStringExtra(Constants.Extra.STORY_ID)
            //上一集的剧集id,只有续写的时候才有
            mRealVM.previousStoryId = hostVm?.previousStoryId
            mRealVM.inspiration = it.getStringExtra(Constants.Extra.CONTENT)
            mRealVM.model = it.getIntExtra(Constants.Extra.MODE, 0)
            mRealVM.customEpisodeCount = it.getStringExtra(StoryEditorActivity.CUSTOM_EPISODE_COUNT)
            mRealVM.episodeRangeCode = it.getStringExtra(StoryEditorActivity.EPISODE_RANGE_CODE)
            mRealVM.videoStyleId = it.getStringExtra(Constants.Extra.STYLE_ID)
            mRealVM.videoRatio = it.getStringExtra(Constants.Extra.VIDEO_RATIO)
            mRealVM.chapterIndex = it.getIntExtra(Constants.Extra.CHAPTER_INDEX, 1)
            scriptGenerate()
        }
        initView()
        //数据更新
        dataUpdate()
    }

    fun scriptGenerate() {
        if (mRealVM.isNovelDocuments()) {
            mRealVM.pollNovelScript()
            showBackToast = true
        } else {
            mRealVM.sseGenerate()
            hostVm?.backWarning = true
        }
    }

    private fun initView() {
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })
        binding.btnRegenerate.setOnSingleClickListener {
            if (mRealVM.isStreaming()) return@setOnSingleClickListener
            scriptGenerate()
        }
        binding.retryBtn.setOnSingleClickListener {
            if (mRealVM.isStreaming()) return@setOnSingleClickListener
            scriptGenerate()
        }
        binding.btnNext.setOnSingleClickListener {
            if (mRealVM.isStreaming()) return@setOnSingleClickListener
            //检查敏感词
            lifecycleScope.launch {
                sensitiveWordCheck(mRealVM.input.value, {
                    mRealVM.confirmGenerate(hostVm?.title?.value) { workId, storyId, taskId ->
                        hostVm?.nextStep(3, workId, storyId, taskId)
                    }
                }, {
                    it?.let {
                        binding.inputView.setText(it)
                    }
                })
            }
        }
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (mRealVM.isStreaming()) return super.onSingleTapConfirmed(e)
                if (mRealVM.input.value?.trim().isNullOrEmpty()) return super.onSingleTapConfirmed(e)
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
    var scrollAnimator: ObjectAnimator? = null
    private fun dataUpdate() {
        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                if (dialog == null || !dialog!!.isShowing) {
                    // 只在 Dialog 不存在时创建
                    dialog = GeneratingDialog(requireContext(), it.second, it.third)
                    if (mRealVM.isNovelDocuments()) {
                        dialog?.setCancelable(true)
                    }
                    dialog?.show()
                } else {
                    // 如果已经在显示，只更新文字内容，不重置 Dialog
                    dialog?.updateContent(it.second, it.third)
                }
            } else {
                dialog?.dismiss()
                dialog = null
            }
        }

        mRealVM.input.observe(this) {
            val newText = it ?: ""
            val currentText = binding.inputView.text.toString()

            if (newText != currentText) {
                // 【优化1：尽量使用增量追加】
                // 如果新文本包含了旧文本（标准的流式追加），使用 append 能避免全量重绘，极大减少闪烁
                if (newText.startsWith(currentText)) {
                    val appendText = newText.substring(currentText.length)
                    binding.inputView.append(appendText)
                } else {
                    // 如果发生了清空或非追加的改变，再使用 setText
                    binding.inputView.setText(newText)
                }

                binding.inputView.post {
                    val layout = binding.inputView.layout ?: return@post

                    val contentHeight = layout.getLineTop(binding.inputView.lineCount)
                    val targetScrollY = contentHeight + binding.inputView.paddingTop + binding.inputView.paddingBottom - binding.inputView.height

                    if (targetScrollY > 0) {
                        // 【优化2：对于高频流式输出，直接 scrollTo 效果往往比动画更好】
                        // 因为文字逐个蹦出本身就自带了动效节奏。直接贴底是最丝滑的。
                        // 如果你想试试无动画的清爽感，可以直接解除下面这行的注释，并删掉动画代码：
                        // binding.inputView.scrollTo(0, targetScrollY)

                        // 【优化3：如果坚持要用动画，必须取消上一次未完成的动画】
                        scrollAnimator?.cancel()
                        scrollAnimator = ObjectAnimator.ofInt(binding.inputView, "scrollY", binding.inputView.scrollY, targetScrollY).apply {
                            // 时长改短，匹配 SSE 频率，避免积压
                            duration = 60
                            // 使用线性插值器，避免默认插值器(加速减速)带来的顿挫感
                            interpolator = android.view.animation.LinearInterpolator()
                            start()
                        }
                    } else {
                        binding.inputView.scrollTo(0, 0)
                    }
                }
            }
        }

        mRealVM.storyScriptResult.observe(this) {
            showBackToast = false
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
    }

    fun getDuringToast(): String {
        val workName = mRealVM.mWorkName.value?.ifBlank { "未命名" } ?: "未命名"
        return "《${workName}》AI正在制作中\n已自动为您保存为草稿"
    }


    override fun handleBackPressed(): Boolean {
        if (showBackToast) {
            getDuringToast().showToast()
        }
        return false
    }
}