package com.kgmedia.fastfilm.feature.play.publish

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.ActivityShortDramaPublishBinding
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorActivity
import com.kgmedia.fastfilm.feature.play.ShortDramaPlayerViewModel
import com.kgmedia.fastfilm.feature.play.more.DownloadTypeDialog
import com.kgmedia.fastfilm.feature.play.more.MoreOptionDialog
import com.kgmedia.fastfilm.feature.play.more.ShareShortDramaActivity
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.kgmedia.fastfilm.kit.PlayerFactory
import com.kgmedia.fastfilm.kit.play
import com.kgmedia.fastfilm.widget.MicroAnimationConfig
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setupTranslucentStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 短剧发布
 */
class ShortDramaPublishActivity : ShareShortDramaActivity<ActivityShortDramaPublishBinding, ShortDramaPlayerViewModel>() {
    companion object {
        fun start(dramaId: String?) {
            val activity = topActivity ?: return
            val intent = Intent(activity, ShortDramaPublishActivity::class.java)
            intent.putExtra(Constants.Extra.DRAMA_ID, dramaId)
            intent.putExtra(Constants.Extra.UNRELEASED, true)
            activity.startActivity(intent)
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.player = PlayerFactory.Companion.createShortPlayer(withCache = true, autoAudioFocus = true)
        mRealVM.dramaDetail.observe(this) {
            mRealVM.player?.play(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mRealVM.eyeProtectionEnabled.observe(this) {
            Config.Companion.get().storeEyeProtectionEnabled(it ?: false)
        }
        mRealVM.currentSegmentPosition.observe(this) {
            val isTop = it % 2 == 1
            binding.imagesFlipper.seekTo(
                it,
                mRealVM.currentSegment.get()?.durationMs,
                if (isTop) MicroAnimationConfig.TranslationDirection.BOTTOM else MicroAnimationConfig.TranslationDirection.TOP
            )
        }
        mRealVM.dramaDetail.observe(this) {
            it.shotSegmentList?.map { segment -> segment.shotSegmentImageUrl }?.let { urls ->
                binding.imagesFlipper.setImageUrls(urls)
                val config = binding.imagesFlipper.getMicroAnimationConfig()
                config.initialScale = calculateScale(it.videoRatio)
                binding.imagesFlipper.setMicroAnimationConfig(config)
                lifecycleScope.launch {
                    delay(200)
                    binding.imagesFlipper.startMicroAnimation(it.shotSegmentList.getOrNull(0)?.durationMs)
                    hasStarted = true
                }
            }
        }

        mRealVM.isPaused.observe(this) { isPaused ->
            if (this@ShortDramaPublishActivity.hasStarted) {
                if (isPaused) {
                    binding.imagesFlipper.pauseMicroAnimation()
                } else {
                    binding.imagesFlipper.resumeMicroAnimation()
                }
            }

        }
        observeEvents {
            on<Events.ShortDramasUpdatedEvent> { event ->
                if (isActive && mRealVM.dramaId == event.dramasId) {
                    mRealVM.loadDramaDetail(true)
                }
            }
            on<Events.ShortDramasEditInfoEvent> { mRealVM.updateShotInfo(it) }
        }
    }

    private fun calculateScale(videoRatio: Int?): Float {
        //[枚举]视频比例 [1=9:16 2=16:9 3=3:4 4=4:3]
        return when (videoRatio) {
            1 -> {
                1.08f
            }

            2 -> {
                1.2f
            }

            3 -> {
                1.12f
            }

            4 -> {
                1.16f
            }

            else -> {
                1.2f
            }
        }
    }

    var hasStarted = false

    // 自定义 PagerAdapter
    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.reedit -> {
                ShortDramaEditorActivity.Companion.start(mRealVM.dramaId)
            }

            R.id.download -> {
                DownloadTypeDialog.Companion.show(
                    this,
                    mRealVM.shortCover.get(),
                    mRealVM.shortName.get(),
                    mRealVM.dramaDetail.value?.artist?.nickName
                )
            }

            R.id.more -> {
                MoreOptionDialog.Companion.show(
                    this,
                    mRealVM.dramaId,
                    mRealVM.shortName.get(),
                    mRealVM.shortIntro.get()
                )
            }

            R.id.publish -> {
                EventManager.onAppTrack(AppEvent.JG_CLICK_SYNTHETIC_VIDEO_PUBLISH_BUTTON)
                mRealVM.publish {
                    finish()
                    SeriesActivity.start(it, true)
                }
            }
        }
    }

    override val exportVideoOverlay: View?
        get() = bindingNullable?.overlay?.overlay

    override fun setTranslucentStatus() {
        setupTranslucentStatus(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealVM.player?.release()
    }
}