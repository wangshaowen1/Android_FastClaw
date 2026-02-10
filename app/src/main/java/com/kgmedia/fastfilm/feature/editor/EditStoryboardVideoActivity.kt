package com.kgmedia.fastfilm.feature.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.databinding.ActivityEditStoryboardVideoBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast

/**
 * 编辑分镜视频
 */
class EditStoryboardVideoActivity : BaseViewModelActivity<ActivityEditStoryboardVideoBinding, EditStoryboardVideoViewModel>() {
    companion object {
        @JvmStatic
        fun launchIntent(activity: Activity, storyTitle: String?, workId: String?, storyId: String?, workRatio: String?): Intent {
            val intent = Intent(activity, EditStoryboardVideoActivity::class.java)
            intent.putExtra(Constants.Extra.TITLE, storyTitle)
            intent.putExtra(Constants.Extra.WORK_ID, workId)
            intent.putExtra(Constants.Extra.STORY_ID, storyId)
            intent.putExtra(Constants.Extra.VIDEO_RATIO, workRatio)
            return intent
        }

        @JvmStatic
        fun start(storyTitle: String?, workId: String?, storyId: String?, workRatio: String?) {
            topActivity?.let { it.startActivity(launchIntent(it, storyTitle, workId, storyId, workRatio)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeEvents {
            on<Events.StoryboardVideoRegenerateEvent> {
                mRealVM.getStoryboardList()
            }
            on<Events.StoryboardImageRegenerateEvent> {
                mRealVM.getStoryboardList()
            }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnRegenerate -> {
                val current = mRealVM.currentStoryboard.get()?.data?.get() ?: return
                if (current.videoGenerating) {
                    "视频正在生成中，请稍后再试".showToast()
                    return
                }
                EditStoryboardActivity.start(current, mRealVM.workRatio)
            }

            R.id.btnDownload -> {
                val current = mRealVM.currentStoryboard.get()?.data?.get() ?: return
                if (current.videoGenerating) {
                    "视频正在生成中，请稍后再试".showToast()
                    return
                }
                if (current.videoGenerateFailed) {
                    "视频生成失败，请重新生成".showToast()
                    return
                }
                if (current.videoGenerated) {
                    current.videoUrl?.let { mRealVM.download(current, it) }
                }
            }

            R.id.btnDelete -> {
                showCommonDialog(tip = "删除后，该分镜视频将永久移除，无法恢复，并可能影响后续视频合成，是否确认删除", confirmText = "确认删除") {
                    mRealVM.deleteCurrentStoryboard()
                }
            }

            R.id.btnGenerate -> {
                mRealVM.synthesis()
            }
        }
    }

}