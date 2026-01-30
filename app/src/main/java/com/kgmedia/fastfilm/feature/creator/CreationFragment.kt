package com.kgmedia.fastfilm.feature.creator

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.view.MotionEvent
import android.view.View
import com.blankj.utilcode.util.ClipboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.ext.onUserSignOut
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.SSEContentBody
import com.kgmedia.fastfilm.databinding.FragmentCreationBinding
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.creator.set.VideoSetDialog
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast

class CreationFragment :
    BaseViewModelFragment<FragmentCreationBinding, CreationViewModel>() {

    companion object {
        const val KEY_MODEL = "model"
        const val MODEL_INSPIRATION = 1
        const val MODEL_CUSTOM = 2
        fun newInstance(model: Int? = null): CreationFragment {
            val fragment = CreationFragment()
            val bundle = Bundle()
            bundle.putInt(KEY_MODEL, model ?: MODEL_INSPIRATION)
            fragment.arguments = bundle
            return fragment
        }
    }

    var dialog: GeneratingDialog? = null
    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        mRealVM.inspirationPrompts.observe(this) {
            binding.lyricPrompts.setTags(it?.mapNotNull { it1 -> it1.configValue }, true)
        }
        binding.lyricPrompts.setOnItemClickListener { _, index ->
            binding.lyricPrompts.tags.getOrNull(index)?.let {
                mRealVM.input.value = it
            }
            false
        }
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
            if (mRealVM.input.value != binding.inputView.text) {
                binding.inputView.setText(mRealVM.input.value)
            }
            if (mRealVM.inspirationModel()) Global.inspirationContent = it else Global.customizedContent = it
        }
//        binding.inputView.setOnTextOutOfBondsListener {
//            mRealVM.maxInputLength.get().takeIf { it > 0 }.let {
//                getString(R.string.toast_input_max_characters_exceeded, it).showToast()
//            }
//        }
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })

        observeEvents {
            on<Events.ConfigChangeEvent> { event ->
                mRealVM.refreshConfig()
            }
            onUserSignOut {
                mRealVM.input.value = ""
            }
            on<Events.ScriptContentGenerateSuccessEvent> {
                if (it.type == Events.ScriptContentGenerateSuccessEvent.TYPE_INSPIRATION || it.type == Events.ScriptContentGenerateSuccessEvent.TYPE_CUSTOMIZED) {
                    mRealVM.input.value = ""
                }
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            mRealVM.refreshConfig()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        binding.inputView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 请求父容器不拦截触摸事件
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_UP -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            // 保持原有事件处理
            false
        }
    }

    fun showVideoSetDialog() {
        mRealVM.videoSetConfig(true) {
            val newInstance = VideoSetDialog.newInstance(mRealVM.videoSetConfig.value)
            newInstance.onSuccess = {
                StoryEditorActivity.start(
                    mActivity,
                    mRealVM.input.value, null, null,
                    if (mRealVM.inspirationModel()) DramaGenerateBody.GEN_WAY_INSPIRATION else DramaGenerateBody.GEN_WAY_CUSTOMIZED
                )
            }
            newInstance.show(childFragmentManager)
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnOptimize -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if ((mRealVM.input.value?.length ?: 0) < mRealVM.minInputLength) {
                    "请输入至少${mRealVM.minInputLength}个字".showToast()
                    return
                }
                if (mRealVM.inspirationModel()) {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_INSPIRATION_STORY_OPTIMIZE_BUTTON)
                } else {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_WRITE_OWN_STORY_OPTIMIZE_BUTTON)
                }
                mRealVM.sseGenerate(SSEContentBody.TYPE_AI_OPTIMIZE_INSPIRATION, "优化中，请稍候...")
            }

            R.id.randomInspirations -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                mRealVM.randomInspirations(true)

            }

            R.id.btnRandomPrompt -> {
                showVideoSetDialog()
                return
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                EventManager.onAppTrack(AppEvent.JG_CLICK_INSPIRATION_STORY_RANDOM_INSPIRATION_BUTTON)
                mRealVM.sseGenerate(SSEContentBody.TYPE_RANDOM_INSPIRATION, "灵感生成中，请稍候...")
            }

            R.id.btnDelete -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                mRealVM.input.value = ""
            }

            R.id.btnPaste -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                mRealVM.input.value = ClipboardUtils.getText().toString()
            }

            R.id.btnNext -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if ((mRealVM.input.value?.length ?: 0) < mRealVM.minInputLength) {
                    "请输入至少${mRealVM.minInputLength}个字".showToast()
                    return
                }
                showVideoSetDialog()
                if (mRealVM.inspirationModel()) {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_INSPIRATION_STORY_NEXT_STEP_BUTTON)
                } else {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_WRITE_OWN_STORY_NEXT_STEP_BUTTON)
                }
            }
        }
    }

}