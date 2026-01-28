package com.kgmedia.fastfilm.feature.play

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.Constants
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
        fun start(dramasId: String?, position: Int? = 0) {
            val context = topActivity ?: return
            val starter = Intent(context, DramasPlayPagerActivity::class.java).apply {
                putExtra(Constants.Extra.DRAMA_ID, dramasId)
                putExtra(Constants.Extra.POSITION, position)
            }
            context.startActivity(starter)
        }
    }

    private val adapter = VideoFeedAdapter(this)
    private lateinit var playerManager: FeedPlayerManager
    private lateinit var listController: FeedListController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dramaId = intent.getStringExtra(Constants.Extra.DRAMA_ID)
        val initialPos = intent.getIntExtra(Constants.Extra.POSITION, 0)

        // 1. 初始化核心组件
        playerManager = FeedPlayerManager(this, lifecycleScope)
        binding.recyclerView.adapter = adapter
        listController = FeedListController(binding.recyclerView, adapter, playerManager)

        // 2. 数据流
        lifecycleScope.launch {
            mRealVM.feedFlow.collectLatest { adapter.submitData(it) }
        }

        // 告知 Controller 初始位置 (需要在 FeedListController 中加个方法，见下文)
        listController.setInitialPosition(initialPos)

        // 加载数据
        // 因为 Repository 是单例且缓存了 Flow，如果列表页也用了 FeedRepository，
        // 这里会立刻拿到数据，不需要网络请求
        mRealVM.loadFeed(true, dramaId)

        // 3. 事件流
        setupPlayerEvents()
    }

    private fun setupPlayerEvents() {
        // 进度同步 (幽灵进度问题)
        lifecycleScope.launch {
            playerManager.progressFlow.collect { (current, total) ->
                updateViewHolderSafely { holder ->
                    holder.updateProgress(current, total)
                }
            }
        }

        // 播放状态同步 (图标)
        lifecycleScope.launch {
            playerManager.isPlayingFlow.collect { isPlaying ->
                updateViewHolderSafely { holder ->
                    holder.updatePlayState(isPlaying)
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

    // [封装] 安全更新 ViewHolder 的辅助方法
    private fun updateViewHolderSafely(action: (VideoFeedAdapter.VideoViewHolder) -> Unit) {
        val pos = listController.getCurrentPosition()
        if (pos >= 0) {
            val holder = binding.recyclerView.findViewHolderForAdapterPosition(pos) as? VideoFeedAdapter.VideoViewHolder
            val item = adapter.peek(pos)

            // [关键] 只有当 ViewHolder 当前展示的数据 ID 与 播放器正在播的 ID 一致时，才允许更新 UI
            if (holder != null && item?.trackId == playerManager.currentPlayingId) {
                action(holder)
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
    override fun onPause() {
        super.onPause(); playerManager.pause()
    }

    override fun onResume() {
        super.onResume(); playerManager.resumeOnForeground()
    }

    override fun onDestroy() {
        super.onDestroy(); playerManager.release()
    }
}