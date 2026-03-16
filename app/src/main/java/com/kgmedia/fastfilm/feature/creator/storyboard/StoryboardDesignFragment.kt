package com.kgmedia.fastfilm.feature.creator.storyboard

import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.Observable
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.applySmartFocusManager
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.FragmentStoryboardDesignBinding
import com.kgmedia.fastfilm.feature.creator.generate.OnBackPressedHandler
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorViewModel
import com.kgmedia.fastfilm.feature.editor.EditStoryboardVideoActivity
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.getParentViewModel
import com.nhsd.basic.ext.showToast

/**
 * 分镜设计
 */
class StoryboardDesignFragment :
    BaseViewModelFragment<FragmentStoryboardDesignBinding, StoryboardDesignViewModel>(), OnBackPressedHandler {

    companion object {
        fun newInstance() = StoryboardDesignFragment()
    }

    private val hostVm by lazy { getParentViewModel<StoryEditorViewModel>() }
    val from by lazy { hostVm?.from }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        mRealVM.workId = hostVm?.workId
        mRealVM.storyId = hostVm?.storyId
        mRealVM.taskId = hostVm?.taskId
        mRealVM.step.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                Global.refreshBalance()
            }
        })
        mRealVM.promptGenerating.observe(this) { (show, title, progress) ->
            bindingNullable?.apply {
                loadingLayout.isVisible = show
                loading.title.text = "$title $progress%"
                loading.tips.text = "退出后AI会继续生成"
            }
        }
        mRealVM.retry()
        EventManager.onAppTrack(AppEvent.SF_VIEW_STORYBOARD_DESIGN_PAGE)
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnNext -> {
                mRealVM.clickNext { workId, storyId, taskId ->
                    EditStoryboardVideoActivity.start(
                        hostVm?.title?.value?.let { "$it：第${hostVm?.chapterIndex ?: 1}集" },
                        workId,
                        storyId,
                        hostVm?.videoRatio,
                        null,
                         from = from
                    )
                    activity?.finish()
                }
            }
        }
    }

    override fun handleBackPressed(): Boolean {
        if (mRealVM.notSaved()) {
            activity?.showCommonDialog(
                tip = "您对分镜信息有修改尚未保存，是否保存修改？",
                cancelText = "不保存",
                cancel = {
                    toSeries()
                    activity?.finish()
                },
                confirmText = "保存并退出",
                confirm = {
                    mRealVM.saveAndFinish {
                        toSeries()
                        activity?.finish()
                    }
                })
            return true
        }
        if (mRealVM.generating) {
            "AI正在生成中，已自动保存为草稿".showToast()
        } else {
            "已自动保存为草稿".showToast()
        }
        toSeries()
        mActivity.finish()
        return true
    }

    fun toSeries() {
        if (from != StoryEditorActivity.FROM_SERIES) {
            SeriesActivity.start(mRealVM.workId)
        }
    }

}