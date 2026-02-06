package com.kgmedia.fastfilm.feature.creator.continuewriting

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.MotionEvent
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.databinding.ActivityContinueWritingBinding
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast

class ContinueWritingActivity :
    BaseViewModelActivity<ActivityContinueWritingBinding, ContinueWritingViewModel>() {

    companion object {

        @JvmStatic
        fun start(context: Context?, workId: String?, workTitle: String?) {
            if (context == null) return
            val starter = Intent(context, ContinueWritingActivity::class.java)
            starter.putExtra(Constants.Extra.ID, workId)
            starter.putExtra(Constants.Extra.NAME, workTitle)
            context.startActivity(starter)
        }
    }

    var dialog: GeneratingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = GeneratingDialog(this, it.second)
                dialog?.show()
            } else {
                dialog?.dismiss()
            }
        }
        mRealVM.input.observe(this) {
            if (mRealVM.input.value != binding.inputView.text) {
                binding.inputView.setText(mRealVM.input.value)
            }
            Global.continueWritingContent[mRealVM.workId] = mRealVM.input.value
        }
        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })
        observeEvents {
            on<Events.ConfigChangeEvent> { event ->
                //mRealVM.refreshConfig()
            }
            on<Events.ScriptContentGenerateSuccessEvent> {
                if (it.type == Events.ScriptContentGenerateSuccessEvent.TYPE_CONTINUE_WRITE && it.seriesId == mRealVM.workId) {
                    finish()
                }
            }
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

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnRegenerate, R.id.btnAIExpand -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if (!mRealVM.isSSEStreaming.get()) {
                    mRealVM.sseGenerate()
                }
            }

            R.id.btnDelete -> {
                mRealVM.input.value = ""
            }

            R.id.btnNext -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if ((mRealVM.input.value?.length ?: 0) < mRealVM.minInputLength) {
                    "请输入至少${mRealVM.minInputLength}个字".showToast()
                    return
                }
                if (mRealVM.episodeInfo.value == null) {
                    mRealVM.getEpisodeInfo {
                        toGenerate()
                    }
                    return
                }
                if (!mRealVM.isSSEStreaming.get()) {
                    toGenerate()
                }
            }
        }
    }

    fun toGenerate() {
        mRealVM.saveSequelNovelStory { saveSequelNovelStory ->
            StoryEditorActivity.startContinue(
                context = this,
                workId = saveSequelNovelStory.workId,
                workTitle = mRealVM.workName,
                storyId = saveSequelNovelStory.storyId,
                previousStoryId = mRealVM.episodeInfo.value?.storyId,
                model = DramaGenerateConstants.MODEL_CONTINUE_STORY,
            )
        }
    }
}