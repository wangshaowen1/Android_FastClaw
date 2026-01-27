package com.kgmedia.fastfilm.feature.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.marginEnd
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.ActivityShortDramaEditorBinding
import com.kgmedia.fastfilm.feature.editor.dubbing.DubbingDialog
import com.kgmedia.fastfilm.feature.editor.history.StoryboardHistoryDialog
import com.kgmedia.fastfilm.feature.editor.regenerate.StoryboardRegenerateDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.showToast

/**
 * 短剧编辑
 */
class ShortDramaEditorActivity : BaseViewModelActivity<ActivityShortDramaEditorBinding, ShortDramaEditorViewModel>() {
    companion object {
        const val EXTRA_UNLOCK = "unlock"

        @JvmStatic
        fun launchIntent(activity: Activity, dramasId: String?, unlock: Boolean = false): Intent {
            val intent = Intent(activity, ShortDramaEditorActivity::class.java)
            intent.putExtra(Constants.Extra.DRAMAS_ID, dramasId)
            intent.putExtra(EXTRA_UNLOCK, unlock)
            return intent
        }

        @JvmStatic
        fun start(dramasId: String?, unlock: Boolean = false) {
            topActivity?.let { it.startActivity(launchIntent(it, dramasId, unlock)) }
        }
    }

    override fun onBackPressed() {
        "已保存至作品".showToast()
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventManager.onAppTrack(AppEvent.JG_VIEW_GENERATE_STORYBOARD_PROCESS_PAGE)
        setupTranslucentStatus(statusBarDarkFont = false, navigationBarColor = R.color.color_121212.getResColor())
        mRealVM.scrollToItem.observe(this) { itemPosition ->
            if (itemPosition > -1 && isActive) {
                mRealVM.scrollToItem.value = -1
                bindingNullable?.rvStoryboard?.apply {
                    val itemView = getChildAt(0) ?: return@apply
                    val scrollX = computeHorizontalScrollOffset()
                    val itemWidth = itemView.width
                    val itemDividerWidth = itemView.marginEnd
                    val left = paddingLeft + itemPosition * (itemWidth + itemDividerWidth) - (width - itemWidth) / 2
                    smoothScrollBy(left - scrollX, 0)
                }
            }
        }
        observeEvents {
            on<Events.ShortDramasUpdatedEvent> { event ->
                if (isActive && event.dramasId == mRealVM.dramasId) {
                    mRealVM.getStoryboardInfo()
                }
            }

            /**
             * 字幕配音事件
             */
            on<Events.DubbingEvent> { event ->
                if (isActive && event.dramasId == mRealVM.dramasId) {
                    mRealVM.loopDubbingStatus(event.timbreId)
                }
            }
            on<Events.UseDubbingEvent> { event ->
                if (isActive && event.dramasId == mRealVM.dramasId) {
                    mRealVM.getStoryboardInfo(true)
                }
            }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnRegenerate -> {
                mRealVM.currentSegment.get()?.data?.get()?.let {
                    if (it.isFailed()) {
                        StoryboardRegenerateDialog.show(supportFragmentManager, mRealVM.dramasId, it)
                    } else if (!it.shotSegmentImageUrl.isNullOrEmpty()) {
                        //生成成功且首次创作未解锁
                        if (mRealVM.shortDrama.get()?.firstGenLocked() == true) {
                            "解锁完整作品后重绘分镜".showToast()
                        } else {
                            StoryboardRegenerateDialog.show(supportFragmentManager, mRealVM.dramasId, it)
                        }
                    } else {
                        "分镜正在生成中，请稍候...".showToast()
                    }
                }
            }

            R.id.btnGenerate -> {
                if (mRealVM.nextStepEnabled.get()) {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_SYNTHESIZE_VIDEO_BUTTON)
                    val shortDrama = mRealVM.shortDrama.get()
                    if (shortDrama?.firstGenLocked() == true) {
                        mRealVM.calcCost()
                    } else {
                        mRealVM.synthesis()
                    }
                } else if (mRealVM.anySegmentGenerating()) {
                    "请完善所有画面".showToast()
                } else if (mRealVM.anySegmentFailed()) {
                    "请完善所有画面".showToast()
                }
            }
        }
    }

}