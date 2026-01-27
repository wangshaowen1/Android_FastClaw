package com.kgmedia.fastfilm.feature.play

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.ActivityDramasPlayPagerBinding
import com.kgmedia.fastfilm.feature.play.manger.FeedPlayerManager
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DramasPlayPagerActivity :
    BaseViewModelActivity<ActivityDramasPlayPagerBinding, DramasPlayPagerViewModel>(),
    VideoFeedAdapter.InteractionListener {

    companion object {
        @JvmStatic
        fun start(dramasId: String?) {
            val context = topActivity ?: return
            val starter = Intent(context, DramasPlayPagerActivity::class.java)
            context.startActivity(starter)
        }
    }

    private val adapter = VideoFeedAdapter(this) // 传入 this
    private lateinit var layoutManager: LinearLayoutManager
    private val snapHelper = PagerSnapHelper()
    private lateinit var playerManager: FeedPlayerManager

    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerManager = FeedPlayerManager(this, lifecycleScope)

        layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
        snapHelper.attachToRecyclerView(binding.recyclerView)

        setupScrollListener()

        lifecycleScope.launch {
            mRealVM.feedFlow.collectLatest { adapter.submitData(it) }
        }
        setupAutoPlayOnFirstLoad()
        setupPlayerEvents()
    }

    private fun setupAutoPlayOnFirstLoad() {
        adapter.addLoadStateListener { loadState ->
            // 当刷新状态变为 "NotLoading" (即加载完成) 且是第一次加载
            if (loadState.refresh is LoadState.NotLoading && isFirstLoad) {
                if (adapter.itemCount > 0) {
                    isFirstLoad = false
                    // 使用 post 确保 RecyclerView 布局已经完成，能找到 SnapView
                    binding.recyclerView.post {
                        autoPlayCurrentItem()
                    }
                }
            }
        }
    }

    private fun setupScrollListener() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    autoPlayCurrentItem()
                }
            }
        })
    }

    private fun autoPlayCurrentItem() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)

        if (position < 0 || position >= adapter.itemCount) return

        val item = adapter.peek(position) ?: return
        val holder = binding.recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder ?: return

        playerManager.attachAndPlay(item, holder.binding.playerView)

        // 预加载
        if (position + 1 < adapter.itemCount) {
            val nextItem = adapter.peek(position + 1)
            nextItem?.musicPlayUrl()?.let { playerManager.preload(it) }
        }
    }

    override fun onTogglePlay(position: Int) {
        Logger.i("Activity", "onTogglePlay")
        playerManager.togglePlay()
    }

    override fun onSeek(position: Int, progress: Long) {
        Logger.i("Activity", "onSeek: $progress")
        playerManager.seekTo(progress)
    }

    private fun setupPlayerEvents() {
        // 1. 进度 -> 更新 Adapter
        lifecycleScope.launch {
            playerManager.progressFlow.collect { (current, total) ->
                val centerView = snapHelper.findSnapView(layoutManager) ?: return@collect
                val holder = binding.recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder
                // 直接调用 holder 的方法更新 Seekbar
                holder?.updateProgress(current, total)
            }
        }

        // 2. 状态 -> 更新 图标
        lifecycleScope.launch {
            playerManager.isPlayingFlow.collect { isPlaying ->
                val centerView = snapHelper.findSnapView(layoutManager) ?: return@collect
                val holder = binding.recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder
                holder?.updatePlayState(isPlaying)
            }
        }

        // 3. 播放结束
        lifecycleScope.launch {
            playerManager.playerEvent.collect { event ->
                if (event is FeedPlayerManager.PlayerEvent.OnPlayEnded) {
                    handlePlayEnded()
                }
            }
        }
    }

    /**
     * 处理播放结束逻辑
     */
    private fun handlePlayEnded() {
        val currentPlayMode = mRealVM.currentPlayMode

        // 模式1：剧集模式 (自动播放下一集)
        if (currentPlayMode == DramasPlayPagerViewModel.PlayMode.SERIES) {
            val currentPos = layoutManager.findFirstVisibleItemPosition()

            // 检查是否有下一集
            if (currentPos < adapter.itemCount - 1) {
                // 模拟用户下滑，RecyclerView 会自动触发 onScrollStateChanged -> SCROLL_STATE_IDLE -> autoPlayCurrentItem
                binding.recyclerView.smoothScrollToPosition(currentPos + 1)
            } else {
                // 已经是最后一集了，重播或者提示用户
                "已经是最后一集了".showToast()
                playerManager.replay()
            }
        }
        // 模式2：推荐/抖音模式 (单曲循环)
        else {
            // 手动触发重播
            playerManager.replay()
        }
    }

    override fun onPause() {
        super.onPause()
        playerManager.pause()
    }

    override fun onResume() {
        super.onResume()
        playerManager.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}