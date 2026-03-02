package com.kgmedia.fastfilm.feature.creator

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.view.MotionEvent
import android.view.View
import com.blankj.utilcode.util.ClipboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.common.utils.SensitiveHighlighter
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.ext.onUserSignOut
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigInfo
import com.kgmedia.fastfilm.core.network.apis.request.SSEContentBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData
import com.kgmedia.fastfilm.databinding.FragmentCreationBinding
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.creator.set.StoryGenerateConfirmDialog
import com.kgmedia.fastfilm.feature.creator.set.VideoSetDialog
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.showToast

class CreationFragment :
    BaseViewModelFragment<FragmentCreationBinding, CreationViewModel>() {

    companion object {
        const val KEY_MODEL = "model"

        //灵感模式
        const val MODEL_INSPIRATION = 1

        //小说改编
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
        //数据更新
        dataUpdate()
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

        binding.lyricPrompts.setOnItemClickListener { _, index ->
            if (mRealVM.isStreaming()) {
                return@setOnItemClickListener true
            }
            val tagConfigCode = binding.lyricPrompts.tags.getOrNull(index)
            mRealVM.inspirationPrompts.value?.forEach { (configCode, configValue) ->
                if (configCode == tagConfigCode) {
                    mRealVM.input.value = configValue
                    return@forEach
                }
            }
            false
        }

        binding.inputView.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mRealVM.input.value = s.toString()
            }
        })
    }

    private fun dataUpdate() {
        mRealVM.inspirationPrompts.observe(this) {
            binding.lyricPrompts.setTags(it?.mapNotNull { it1 -> it1.configCode }, true)
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
            if (mRealVM.inspirationModel()) Global.inspirationContent =
                it else Global.customizedContent = it
        }

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
            showVideoSetDialog()
            if (mRealVM.inspirationModel()) {
                EventManager.onAppTrack(AppEvent.JG_CLICK_INSPIRATION_STORY_NEXT_STEP_BUTTON)
            } else {
                EventManager.onAppTrack(AppEvent.JG_CLICK_WRITE_OWN_STORY_NEXT_STEP_BUTTON)
            }
        }
    }

    fun showVideoSetDialog(fileUrl: String? = null) {
        mRealVM.videoSetConfig(true) {
            val config = mRealVM.videoSetConfig.value
            VideoSetDialog.newInstance(config).apply {
                onSuccess = { range, custom, ratio, style, name ->

                    val isFileModel = !fileUrl.isNullOrEmpty()
                    val content = this@CreationFragment.mRealVM.input.value

                    val singleModel = range == VideoConfigInfo.MODE_SINGLE || (range == VideoConfigInfo.MODE_CUSTOM && custom?.toIntOrNull() == 1)
                    val modelType = when {
                        singleModel -> DramaGenerateConstants.MODEL_STORY_SINGLE
                        this@CreationFragment.mRealVM.inspirationModel() -> DramaGenerateConstants.MODEL_INSPIRATION
                        isFileModel -> DramaGenerateConstants.MODEL_STORY_DOCUMENTS
                        else -> DramaGenerateConstants.MODEL_STORY
                    }

                    val startEditor: (UploadStoryData?) -> Unit = { data ->
                        //只有小说文档模式有data
                        StoryEditorActivity.start(
                            context = mActivity,
                            workId = data?.workId,
                            content = content,
                            name = name,
                            episodeRangeCode = range,
                            customEpisodeCount = custom,
                            videoRatio = ratio,
                            styleId = style,
                            model = modelType
                        )
                    }

                    if (isFileModel) {
                        if ((this@CreationFragment.mRealVM.analysisResult?.charCount ?: 0) > 10000) {//大于10000字要显示确认算力弹窗  这里要拿workid去轮询
                            StoryGenerateConfirmDialog.newInstance(
                                this@CreationFragment.mRealVM.fileUrl,
                                this@CreationFragment.mRealVM.analysisResult,
                                name,
                                range,
                                custom,
                                ratio,
                                style
                            ).apply {
                                    onConfirm = { data -> startEditor(data) }
                                }.show(this@CreationFragment.childFragmentManager)
                        } else {//小于10000字直接开始 这里要拿workid去轮询
                            this@CreationFragment.mRealVM.confirmPowerConsume { data ->
                                if (data?.checkStatus() == true) {
                                    startEditor(data)
                                } else {
                                    PowerRechargeDialog.show()
                                }
                            }
                        }
                    } else {//非小说文档走这里
                        startEditor(null)
                    }
                }
            }.show(childFragmentManager)
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
                if (mRealVM.isStreaming()) {
                    return
                }
                if (mRealVM.inspirationModel()) {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_INSPIRATION_STORY_OPTIMIZE_BUTTON)
                } else {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_WRITE_OWN_STORY_OPTIMIZE_BUTTON)
                }
                mRealVM.sseGenerate(SSEContentBody.TYPE_AI_OPTIMIZE_INSPIRATION, "优化中...")
            }

            R.id.randomInspirations -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                mRealVM.randomInspirations(true)
            }

            R.id.btnRandomPrompt -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if (mRealVM.isStreaming()) {
                    return
                }
                EventManager.onAppTrack(AppEvent.JG_CLICK_INSPIRATION_STORY_RANDOM_INSPIRATION_BUTTON)
                mRealVM.sseGenerate(SSEContentBody.TYPE_RANDOM_INSPIRATION, "灵感生成中...")
            }

            R.id.btnDelete -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if (mRealVM.isStreaming()) {
                    return
                }
                mRealVM.input.value = ""
            }

            R.id.btnStoryUpload -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                StoryUploadDialog.show(
                    mRealVM.fileName,
                    mRealVM.fileUrl,
                    mRealVM.analysisResult
                ) { fileName, fileUrl, analysisResult ->
                    mRealVM.fileName = fileName
                    mRealVM.fileUrl = fileUrl
                    mRealVM.analysisResult = analysisResult
                    showVideoSetDialog(mRealVM.fileUrl)
                }
            }

            R.id.btnPaste -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if (mRealVM.isStreaming()) {
                    return
                }
                mRealVM.input.value = ClipboardUtils.getText().toString()
            }

            R.id.btnNext -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if (mRealVM.isStreaming()) {
                    return
                }
                if ((mRealVM.input.value?.length ?: 0) < mRealVM.minInputLength) {
                    "请输入至少${mRealVM.minInputLength}个字".showToast()
                    return
                }
                //检查敏感词
                mRealVM.sensitiveWordJudgment()
            }
        }
    }
}