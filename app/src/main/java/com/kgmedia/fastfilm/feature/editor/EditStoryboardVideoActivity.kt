package com.kgmedia.fastfilm.feature.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.permissions.requestRead2WritePermissions
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.databinding.ActivityEditStoryboardVideoBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.showToast

/**
 * 编辑分镜视频
 */
class EditStoryboardVideoActivity : BaseViewModelActivity<ActivityEditStoryboardVideoBinding, EditStoryboardVideoViewModel>() {
    companion object {
        @JvmStatic
        fun launchIntent(activity: Activity, storyTitle: String?, workId: String?, storyId: String?, workRatio: String?, story: EpisodeInfo?): Intent {
            val intent = Intent(activity, EditStoryboardVideoActivity::class.java)
            intent.putExtra(Constants.Extra.TITLE, storyTitle)
            intent.putExtra(Constants.Extra.WORK_ID, workId)
            intent.putExtra(Constants.Extra.STORY_ID, storyId)
            intent.putExtra(Constants.Extra.VIDEO_RATIO, workRatio)
            intent.putExtra(Constants.Extra.DATA, story)
            return intent
        }

        @JvmStatic
        fun start(storyTitle: String?, workId: String?, storyId: String?, workRatio: String?, story: EpisodeInfo?) {
            topActivity?.let { it.startActivity(launchIntent(it, storyTitle, workId, storyId, workRatio, story)) }
        }
    }

    override fun onBackPressed() {
        mRealVM.getStoryInfo {
            //重绘已完成
            if (it?.stepProgress == EpisodeInfo.EPISODE_REPAINT_COMPLETED) {
                showCommonDialog(
                    title = "确认退出重绘？",
                    tip = "当前重绘分镜尚未合成完整视频，退出后本次重绘将不会保留。是否继续退出?",
                    cancelText = "确认退出",
                    cancel = {
                        mRealVM.dropRepaint()
                    },
                    confirmText = "继续重绘",
                    confirm = {})
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.loadState.setNoDataButton("生成分镜视频") {
            mRealVM.regenerateStoryboards()
        }
        mRealVM.scrollToItem.observe(this) { itemPosition ->
            if (itemPosition > -1 && isActive) {
                mRealVM.scrollToItem.value = -1
                bindingNullable?.rvStoryboard?.apply {
                    val itemView = getChildAt(0) ?: return@apply
                    val scrollX = computeHorizontalScrollOffset()
                    val itemWidth = itemView.width
                    val itemDividerWidth = itemView.marginStart + itemView.marginEnd
                    val left = paddingLeft + itemPosition * (itemWidth + itemDividerWidth) + itemView.marginStart - (width - itemWidth) / 2
                    smoothScrollBy(left - scrollX, 0)
                }
            }
        }
        observeEvents {
            on<Events.StoryboardSavedEvent> {
                mRealVM.updateStoryboard(it.storyboardInfo)
            }
            on<Events.StoryboardVideoRegenerateEvent> {
                mRealVM.getStoryboardList()
            }
            on<Events.StoryboardImageRegenerateEvent> {
                mRealVM.getStoryboardList()
            }
        }
        EventManager.onAppTrack(AppEvent.SF_VIEW_VIDEO_GENERATION_PAGE)
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
                EditStoryboardActivity.start(current, mRealVM.workRatio, mRealVM.storyInfo?.stepProgress)
                EventManager.onAppTrack(AppEvent.SF_CLICK_REDRAW_STORYBOARD_BUTTON)
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
                    requestRead2WritePermissions { granted ->
                        if (granted) {
                            current.videoUrl?.let { mRealVM.download(current, it) }
                        }
                    }
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