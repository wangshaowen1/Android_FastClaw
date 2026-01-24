package com.kgmedia.fastfilm.feature.play

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants.Extra
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.databinding.FragmentShortDramaPlayerBinding
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorActivity
import com.kgmedia.fastfilm.feature.play.more.ShareShortDramaActivity
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.kgmedia.fastfilm.kit.play
import com.kgmedia.fastfilm.kit.setupMediaSourceManager
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.widget.MicroAnimationConfig
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.getViewModelOfParentFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 短剧播放
 */
class ShortDramaPlayerFragment :
    BaseViewModelFragment<FragmentShortDramaPlayerBinding, ShortDramaPlayerViewModel>() {
    companion object {
        fun newInstance(
            dramaId: String?,
            playerInSeries: Boolean = false,
            unlockCheck: Boolean = false
        ): ShortDramaPlayerFragment {
            val args = Bundle()
            args.putString(Extra.DRAMA_ID, dramaId)
            args.putBoolean(Extra.PLAYER_IN_SERIES, playerInSeries)
            args.putBoolean(Extra.UNLOCK_CHECK, unlockCheck)
            val fragment = ShortDramaPlayerFragment()
            fragment.arguments = args
            return fragment
        }
    }

    val unlockCheck by lazy {
        arguments?.getBoolean(Extra.UNLOCK_CHECK, false)
    }

    val parentVmOfPager by lazy {
        getViewModelOfParentFragment() ?: getViewModelOfActivity() as? ShortDramaPlayPagerViewModel
    }
    val parentVmOfHost by lazy { getViewModelOfActivity() as? ShortDramaPlayerHostViewModel }
    override fun onViewModelCreated() {
        super.onViewModelCreated()
        if (parentVmOfPager != null) {
            mRealVM.player = parentVmOfPager?.shortsPlayer?.delegate?.player
            mRealVM.manager = parentVmOfPager?.shortsPlayer?.setupMediaSourceManager()
        } else {
            mRealVM.player = parentVmOfHost?.player
        }

    }

    private var hasCheckedUnlock = false

    override fun onPrepareView() {
        super.onPrepareView()
        if (mRealVM.playerInSeries) {
            EventManager.onAppTrack(AppEvent.JG_VIEW_HOME_RECOMMEND_VIDEO, mapOf(GrowingIO.KEY_ALL_GENERAL_ID to mRealVM.dramaId))
        }
        mRealVM.autoUnlock.observe(this) {
            parentVmOfPager?.updateAutoUnlock(mRealVM.dramasSeries.get()?.dramasSeriesId, it)
        }
        mRealVM.eyeProtectionEnabled.observe(this) {
            Config.get().storeEyeProtectionEnabled(it ?: false)
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
                    if (mRealVM.player?.currentShort()?.isLocked() == false) {
                        binding.imagesFlipper.startMicroAnimation(it.shotSegmentList.getOrNull(0)?.durationMs)
                        hasStarted = true
                    }
                }
            }
            if (unlockCheck == true && !hasCheckedUnlock && it.isLocked()) {
                if (it.dramasSeries?.autoUnlockStatus == 0) {
                    hasCheckedUnlock = true
                    lifecycleScope.launch {
                        delay(100)
                        showUnlockDialog()
                    }
                } else if (it.dramasSeries?.autoUnlockStatus == 1 && !User.get().checkBalance(it.unlockInfo?.unlockPrice)) {
                    hasCheckedUnlock = true
                    lifecycleScope.launch {
                        delay(100)
                        showUnlockDialog()
                    }
                }
            }
        }

        mRealVM.isPaused.observe(this) { isPaused ->
            if (this@ShortDramaPlayerFragment.hasStarted) {
                if (isPaused) {
                    binding.imagesFlipper.pauseMicroAnimation()
                } else {
                    binding.imagesFlipper.resumeMicroAnimation()
                }
            }
        }
        binding.target.let {
            binding.blurView.setupWith(it)
                .setBlurRadius(5f) // 初始模糊半径（0-25）
                .setBlurAutoUpdate(false)
        }
        mRealVM.isLocked.observe(this) {
            if (!it) {
                mRealVM.player?.play(mRealVM.dramaDetail.value)
                return@observe
            }
        }
        observeEvents {
            on<Events.ShortDramasEditInfoEvent> { mRealVM.updateShotInfo(it) }
            on<Events.SeriesWatchingEvent> { mRealVM.updateShotInfo(it) }
            on<Events.UnLockEvent> { event ->
                if (event.dramasId == mRealVM.dramaId) {
                    mRealVM.dramaDetail.value?.unlockInfo?.unlockStatus = 1
                    if (mRealVM.isLocked.value == true) {
                        mRealVM.isLocked.value = false
                    }
                }
            }
            on<Events.AutoUnLockSwitchEvent> { event ->
                if (event.dramasSeriesId == mRealVM.dramasSeries.get()?.dramasSeriesId) {
                    mRealVM.setAutoUnlock(event.autoLock)
                    mRealVM.dramasSeries.get()?.autoUnlockStatus = if (event.autoLock) 1 else 0
                }
            }
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
    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.reedit -> {
                ShortDramaEditorActivity.start(mRealVM.dramaId)
            }

            R.id.unlock -> {
                if (AppNavigator.checkLoginIntercepted(true)) return
                showUnlockDialog()
            }

            R.id.follow -> {
                if (AppNavigator.checkLoginIntercepted(true)) return
                if (mRealVM.isFollowed.get() == false) {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_PLAY_VIEW_SUBSCRIBE_BUTTON, mapOf(GrowingIO.KEY_ALL_GENERAL_ID to mRealVM.dramaId))
                }
                mRealVM.toggleFollow()
            }

            R.id.likes -> {
                if (AppNavigator.checkLoginIntercepted(true)) return
                if (mRealVM.isLiked.get() == false) {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_PLAY_VIEW_LIKE_BUTTON, mapOf(GrowingIO.KEY_ALL_GENERAL_ID to mRealVM.dramaId))
                }
                mRealVM.toggleLike()
            }

            R.id.seriesLayout -> {
                if (AppNavigator.checkLoginIntercepted(true)) return
                mRealVM.dramasSeries.get()?.let {
                    EventManager.onAppTrack(AppEvent.JG_CLICK_PLAY_VIEW_SPREAD_CHAPTER_BUTTON, mapOf(GrowingIO.KEY_ALL_GENERAL_ID to mRealVM.dramaId))
                    SeriesShortsDialog.show(childFragmentManager, it, mRealVM.playerInSeries).apply {
                        unTotalWorkNumChange = { totalWorkNum ->
                            this@ShortDramaPlayerFragment.mRealVM.dramasSeries.get()?.totalWorkNum = totalWorkNum
                            this@ShortDramaPlayerFragment.mRealVM.totalWorkNum.set(totalWorkNum)
                        }
                    }
                }
            }

            R.id.share -> {
                if (AppNavigator.checkLoginIntercepted(true)) return
                EventManager.onAppTrack(AppEvent.JG_CLICK_PLAY_VIEW_SHARE_BUTTON, mapOf(GrowingIO.KEY_ALL_GENERAL_ID to mRealVM.dramaId))
                (activity as? ShareShortDramaActivity<*, *>)?.shareShortDrama(mRealVM.dramaDetail.value)
            }

            R.id.title -> {
                if (AppNavigator.checkLoginIntercepted(true)) return
                mRealVM.dramaDetail.value?.dramasSeries?.dramasSeriesId?.let {
                    SeriesActivity.start(it)
                }
            }
        }
    }

    private fun showUnlockDialog() {
        mRealVM.dramaDetail.value?.apply {
            UnlockSeriesShortDialog.show(childFragmentManager, workNo, isLocked(), unlockInfo?.unlockPrice).apply {
                unlock = { isAutoUnlock ->
                    this@ShortDramaPlayerFragment.mRealVM.setAutoUnlock(isAutoUnlock)
                    this@ShortDramaPlayerFragment.mRealVM.unlock(true)
                }
            }
        }
    }
}