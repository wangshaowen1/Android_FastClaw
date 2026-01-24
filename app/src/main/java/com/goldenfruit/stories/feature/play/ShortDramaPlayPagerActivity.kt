package com.kgmedia.fastfilm.feature.play

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants.Extra
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.PlaylistDataTransfer
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.databinding.ActivityShortDramaPlayPagerBinding
import com.kgmedia.fastfilm.feature.play.more.ShareShortDramaActivity
import com.kgmedia.fastfilm.kit.setupMediaSourceManager
import com.kgmedia.fastfilm.kit.source.MediaSourceManager
import com.kgmedia.fastfilm.kit.source.impl.ReleasedShortDetailSource
import com.kgmedia.fastfilm.kit.source.impl.SeriesShortsPageSource
import com.kgmedia.fastfilm.widget.NeedUpdateFragmentStateAdapter
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.launch

/**
 * 短剧播放
 */
class ShortDramaPlayPagerActivity :
    ShareShortDramaActivity<ActivityShortDramaPlayPagerBinding, ShortDramaPlayPagerViewModel>() {
    companion object {
        fun start(seriesId: String?, position: Int = 0, unlockCheck: Boolean = false) {
            val activity = topActivity ?: return
            val intent = Intent(activity, ShortDramaPlayPagerActivity::class.java)
            intent.putExtra(Extra.SERIES_ID, seriesId)
            intent.putExtra(Extra.POSITION, position)
            intent.putExtra(Extra.UNLOCK_CHECK, unlockCheck)
            activity.startActivity(intent)
        }
    }

    val unlockCheck by lazy {
        intent.getBooleanExtra(Extra.UNLOCK_CHECK, false)
    }

    private val adapter by lazy {
        MyFragmentStateAdapter(unlockCheck).apply {
            mRealVM.playlist?.let {
                submitData(it)
                if (itemCount > 0) {
                    // 只调用 skipTo，内部会自动处理播放，且不会触发任务取消
                    mRealVM.shortsPlayer.skipTo(mRealVM.initPosition)
                    // 如果需要根据锁状态暂停，可以在这里判断，但不要显式调用 play() 除非确认当前不在加载中
                    if (mRealVM.shortsPlayer.currentShort()?.isLocked() == true) {
                        mRealVM.shortsPlayer.pause()
                    }else {
                        mRealVM.shortsPlayer.play()
                    }
                }
            }
        }
    }
    var manager: MediaSourceManager? = null
    private var hasHandledInitPosition = false
    override fun onViewModelCreated() {
        super.onViewModelCreated()

        // 1. 获取传递过来的数据
        val initialPlaylist = PlaylistDataTransfer.getAndClear()

        // 2. 初始化 Manager
        val manager = mRealVM.shortsPlayer.setupMediaSourceManager().setPlayableSource(ReleasedShortDetailSource())
            .apply {
                lifecycleScope.launch {
                    loadErrorFlow.collect {
                        if (mRealVM.playlist.isNullOrEmpty()) {
                            binding.loadingView.showRetry(it)
                        }
                    }
                }
            }

        // 3. 关键分歧处理：有初始数据 vs 无初始数据
        if (!initialPlaylist.isNullOrEmpty()) {
            // 【情况 A】
            manager.mediaSource = SeriesShortsPageSource(mRealVM.seriesId)
            mRealVM.shortsPlayer.playlist = initialPlaylist
            manager.setInitialMedias(initialPlaylist)
            mRealVM.shortsPlayer.skipTo(mRealVM.initPosition)
            if (initialPlaylist.getOrNull(mRealVM.initPosition)?.isLocked() == true) {
                mRealVM.shortsPlayer.pause()
            }else {
                mRealVM.shortsPlayer.play()
            }
            adapter.submitData(initialPlaylist)
        } else {
            // 【情况 B：无传递数据（例如被系统回收后重建，或者DeepLink进来）】
            // 走标准流程，这会触发 resetState() 并自动发起 loadNextPage() 请求
            manager.setPageSize(mRealVM.minPageSize())
                .setMediaSource(SeriesShortsPageSource(mRealVM.seriesId))
        }
        // 4. 绑定 Manager 到 Activity 变量和 Flow 监听 (保持原有逻辑)
        this.manager = manager
        lifecycleScope.launch {
            manager.newPageDataFlow.collect { added ->
                if (mRealVM.shortsPlayer.playlist.isNullOrEmpty()) {
                    binding.loadingView.showEmpty()
                } else {
                    binding.loadingView.showContent()
                }
                if (adapter.itemCount == 0 && added.getOrNull(mRealVM.initPosition)?.isLocked() == false) {
                    mRealVM.shortsPlayer.skipTo(mRealVM.initPosition)
                    mRealVM.shortsPlayer.play()
                }
                mRealVM.playlist?.let { adapter.submitData(it) }

            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.loadingView.binding.icon.alpha = 0.5f
        binding.loadingView.binding.errorPromptView.setTextColor(R.color.color_8f8f8f.getResColor())
        binding.loadingView.binding.noDataIcon.alpha = 0.5f
        if (mRealVM.playlist?.isEmpty() == true) {
            binding.loadingView.showLoading()
        } else {
            binding.loadingView.showContent()
        }
        binding.loadingView.setRetryListener {
            binding.loadingView.showLoading()
            manager?.retry()
        }
        binding.viewPager2.orientation = ViewPager2.ORIENTATION_VERTICAL
        Logger.i("onCreate", "adapter size=${adapter.itemCount}")
        binding.viewPager2.adapter = adapter
        trySeekToInitPosition(mRealVM.initPosition)
        binding.viewPager2.registerOnPageChangeCallback(callback)
        mRealVM.checkedShort.observe(this) {
            val index = mRealVM.playlist?.indexOfFirst { item -> item.dramasId == it?.dramasId }
            Logger.i("trySeekToInitPosition", "index=${index}   current = ${binding.viewPager2.currentItem}")
            if (index != null && index >= 0 && binding.viewPager2.currentItem != index) {
                binding.viewPager2.setCurrentItem(index, false)
            }
        }

        observeEvents {
            on<Events.SeriesWatchingEvent> { event ->
                // 1. 获取当前播放列表
                val currentPlaylist = mRealVM.playlist
                // 2. 遍历列表，批量更新所有属于该 Series 的集数
                currentPlaylist?.forEach { item ->
                    if (item.dramasSeries?.dramasSeriesId == event.seriesId) {
                        // 更新内存中的数据对象
                        item.interaction?.watchingStatus = event.watchingStatus
                        item.interaction?.watchingNumStr = event.watchingNumStr
                    }
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if ((mRealVM.playlist?.size ?: 0) > 0) {
                adapter.onPageSelected(position)
                val realPosition = adapter.realPosition(position)
                val skipTo = mRealVM.playlist?.getOrNull(realPosition)
                Logger.i("检查数据", "currentShort=${mRealVM.shortsPlayer.currentShort()?.dramasName}   skipTo=${skipTo?.dramasName}")
                if (mRealVM.shortsPlayer.currentShort()?.dramasId != skipTo?.dramasId) {
                    mRealVM.shortsPlayer.skipTo(realPosition)
                    if (skipTo?.unlockInfo?.isLocked() == true) {
                        mRealVM.shortsPlayer.pause()
                    }else {
                        mRealVM.shortsPlayer.play()
                    }
                }
                mRealVM.setNavigationTextByPosition(position)
            }
        }
    }

    /**
     * 核心逻辑：尝试跳转到初始位置
     * 只有在【未跳转过】且【Adapter中有足够数据】时才会执行
     */
    private fun trySeekToInitPosition(position: Int) {
        if (hasHandledInitPosition) return
        // 确保 Adapter 已经有数据，并且目标位置在合法范围内
        Logger.i("trySeekToInitPosition", "adapter size=${adapter.itemCount}   initPosi = ${mRealVM.initPosition}")
        if (adapter.itemCount > mRealVM.initPosition) {
            binding.viewPager2.setCurrentItem(position, false)
            hasHandledInitPosition = true
        }
    }

    inner class MyFragmentStateAdapter(val unlockCheck1: Boolean) :
        NeedUpdateFragmentStateAdapter<ShortDramaInfo?>(
            supportFragmentManager,
            lifecycle,
            false
        ) {

        override fun createFragment(position: Int, item: ShortDramaInfo?): Fragment {
            return ShortDramaPlayerFragment.newInstance(
                item?.dramasId, true, unlockCheck1
            )
        }

        override fun isDiff(old: ShortDramaInfo?, new: ShortDramaInfo?): Boolean {
            return old?.dramasId != new?.dramasId
        }


    }

    override fun setTranslucentStatus() {
        setupTranslucentStatus(false)
    }


    override val exportVideoOverlay: View?
        get() = bindingNullable?.overlay?.root
}