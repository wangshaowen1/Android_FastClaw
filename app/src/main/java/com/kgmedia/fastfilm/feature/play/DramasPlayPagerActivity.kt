package com.kgmedia.fastfilm.feature.play

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.ActivityDramasPlayPagerBinding
import com.kgmedia.fastfilm.feature.play.manger.FeedListController
import com.kgmedia.fastfilm.feature.play.manger.FeedPlayerManager
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
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

    private val adapter = VideoFeedAdapter(this)
    private lateinit var playerManager: FeedPlayerManager
    private lateinit var listController: FeedListController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化核心组件
        playerManager = FeedPlayerManager(this, lifecycleScope)
        binding.recyclerView.adapter = adapter
        listController = FeedListController(binding.recyclerView, adapter, playerManager)

        // 2. 数据流
        lifecycleScope.launch {
            mRealVM.feedFlow.collectLatest { adapter.submitData(it) }
        }

        // 3. 事件流
        setupPlayerEvents()
    }

    private fun setupPlayerEvents() {
        // 进度同步 (幽灵进度问题)
        lifecycleScope.launch {
            playerManager.progressFlow.collect { (current, total) ->
                val pos = listController.getCurrentPosition()
                if (pos >= 0) {
                    val holder = binding.recyclerView.findViewHolderForAdapterPosition(pos) as? VideoFeedAdapter.VideoViewHolder
                    val item = adapter.peek(pos)
                    // [双重校验] 只有 View 位置对，且数据 ID 对，才更新进度
                    if (holder != null && item?.trackId == playerManager.currentPlayingId) {
                        holder.updateProgress(current, total)
                    }
                }
            }
        }

        // 播放状态同步 (图标)
        lifecycleScope.launch {
            playerManager.isPlayingFlow.collect { isPlaying ->
                val pos = listController.getCurrentPosition()
                if (pos >= 0) {
                    val holder = binding.recyclerView.findViewHolderForAdapterPosition(pos) as? VideoFeedAdapter.VideoViewHolder
                    holder?.updatePlayState(isPlaying)
                }
            }
        }

        // 播放结束策略
        lifecycleScope.launch {
            playerManager.playerEvent.collect { event ->
                if (event is FeedPlayerManager.PlayerEvent.OnPlayEnded) handlePlayEnded()
            }
        }
    }

    private fun handlePlayEnded() {
        if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.SERIES) {
            if (!listController.smoothScrollToNext()) {
                "已经是最后一集了".showToast()
                playerManager.replay()
            }
        } else {
            playerManager.replay()
        }
    }

    // --- 交互回调 ---
    override fun onTogglePlay(position: Int) = playerManager.togglePlay()
    override fun onSeek(position: Int, progress: Long) = playerManager.seekTo(progress)

    // --- 生命周期 ---
    override fun onPause() { super.onPause(); playerManager.pause() }
    override fun onResume() { super.onResume(); playerManager.resumeOnForeground() }
    override fun onDestroy() { super.onDestroy(); playerManager.release() }
}