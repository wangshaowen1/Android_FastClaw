package com.kgmedia.fastfilm.feature.play

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.ActivityDramasPlayPagerBinding
import com.kgmedia.fastfilm.feature.play.manger.FeedPlayerManager
import com.nhsd.basic.BaseViewModelActivity
import kotlinx.coroutines.launch

/**
 * @Introduce:创作根界面
 */
class DramasPlayPagerActivity :
    BaseViewModelActivity<ActivityDramasPlayPagerBinding, DramasPlayPagerViewModel>() {

    companion object {
        @JvmStatic
        fun start(dramasId: String?) {
            val context = topActivity ?: return
            val starter = Intent(context, DramasPlayPagerActivity::class.java)
            context.startActivity(starter)
        }
    }

    private val adapter = VideoFeedAdapter()
    private lateinit var layoutManager: LinearLayoutManager
    private val snapHelper = PagerSnapHelper()

    // 播放管理器
    private lateinit var playerManager: FeedPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化 PlayerManager
        playerManager = FeedPlayerManager(this, lifecycleScope)

        // 2. 初始化 RecyclerView
        layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
        // 关键：PagerSnapHelper 实现一页一停
        snapHelper.attachToRecyclerView(binding.recyclerView)

        // 3. 核心：监听滑动
        setupScrollListener()

        // 4. 监听数据
        lifecycleScope.launch {
            // 假设这是从 ViewModel 获取的 Flow
            // viewModel.feedFlow.collectLatest { adapter.submitData(it) }
        }

        // 5. 监听播放器事件 (进度、自动切集)
        setupPlayerEvents()
    }

    private fun setupScrollListener() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                // 只有滑动完全停止时，才触发播放
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    autoPlayCurrentItem()
                }
            }
        })
    }

    private fun autoPlayCurrentItem() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)

        val item = adapter.peek(position) ?: return
        val holder = binding.recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder ?: return

        // 核心：直接传入 CoverPlayerView
        playerManager.attachAndPlay(item, holder.playerView)

        // 预加载下一集
        val nextItem = adapter.peek(position + 1)
        nextItem?.musicPlayUrl()?.let { playerManager.preload(it) }
    }

    private fun setupPlayerEvents() {
        // 1. 监听进度，更新 UI
        lifecycleScope.launch {
            playerManager.progressFlow.collect { (current, total) ->
                val centerView = snapHelper.findSnapView(layoutManager) ?: return@collect
                val holder = binding.recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder
                holder?.updateProgress(current, total)
            }
        }

        // 2. 监听播放状态，处理自动切集
        lifecycleScope.launch {
            playerManager.playerEvent.collect { event ->
                when (event) {
                    is FeedPlayerManager.PlayerEvent.OnPlayEnded -> {
                        handlePlayEnded()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun handlePlayEnded() {
        if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.SERIES) {
            // --- 剧集模式：自动滑到下一集 ---
            val currentPos = layoutManager.findFirstVisibleItemPosition()
            if (currentPos < adapter.itemCount - 1) {
                // 模拟滑动，RecyclerView 会自动触发 onScrollStateChanged -> 触发 autoPlayCurrentItem
                binding.recyclerView.smoothScrollToPosition(currentPos + 1)
            } else {
                // 最后一集，停止或循环
                playerManager.replay()
            }
        } else {
            // --- 抖音模式：单曲循环 ---
            playerManager.replay()
        }
    }

    override fun onPause() {
        super.onPause()
        playerManager.pause()
    }

    override fun onResume() {
        super.onResume()
        // 页面回来时，恢复播放
        playerManager.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }


}