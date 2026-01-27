package com.kgmedia.fastfilm.feature.play.manger

import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.feature.play.VideoFeedAdapter

/**
 * 列表控制器：负责滑动监听、自动播放、预加载
 */
class FeedListController(
    private val recyclerView: RecyclerView,
    private val adapter: VideoFeedAdapter,
    private val playerManager: FeedPlayerManager
) {
    private val layoutManager = LinearLayoutManager(recyclerView.context)
    private val snapHelper = PagerSnapHelper()
    private var isFirstLoad = true

    init {
        recyclerView.layoutManager = layoutManager
        snapHelper.attachToRecyclerView(recyclerView)

        // 1. 滑动监听
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    playCurrentItem()
                }
            }
        })

        // 2. 首屏加载监听
        adapter.addLoadStateListener { loadState ->
            if (loadState.refresh is LoadState.NotLoading && isFirstLoad) {
                if (adapter.itemCount > 0) {
                    isFirstLoad = false
                    // Post 确保 Layout 完成
                    recyclerView.post {
                        if (recyclerView.isAttachedToWindow) playCurrentItem()
                    }
                }
            }
        }
    }

    fun playCurrentItem() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)

        if (position < 0 || position >= adapter.itemCount) return

        val item = adapter.peek(position) ?: return
        val holder = recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder ?: return

        // 调用 Manager 播放
        playerManager.attachAndPlay(item, holder.binding.playerView)
        holder.updatePlayState(true)

        // 预加载下一条
        if (position + 1 < adapter.itemCount) {
            val nextItem = adapter.peek(position + 1)
            nextItem?.playUrl?.let { playerManager.preload(it) }
        }
    }

    fun smoothScrollToNext(): Boolean {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return false
        val currentPos = layoutManager.getPosition(centerView)

        if (currentPos < adapter.itemCount - 1) {
            recyclerView.smoothScrollToPosition(currentPos + 1)
            return true
        }
        return false
    }

    fun getCurrentPosition(): Int {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return -1
        return layoutManager.getPosition(centerView)
    }
}